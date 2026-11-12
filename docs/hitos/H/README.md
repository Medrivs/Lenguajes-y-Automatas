# Hito H — Optimización de código intermedio TAC

## Objetivo

Simplificar los cuádruplos del **Hito G** sin alterar los resultados observables del programa Pascal. En este hito se implementan el **plegado de constantes**, la **propagación local de constantes**, las **identidades algebraicas seguras**, la **eliminación de temporales muertos** y la **eliminación de código inalcanzable**. El código ensamblador corresponde al siguiente hito.

Este optimizador utiliza las clases `TacInstruction` y `TacGenerator` ya existentes; **no reemplaza el AST ni reescribe el parser**. Al igual que el proyecto, está hecho en **Java 21**, con nombres de clases y métodos en inglés y comentarios/mensajes en español.

## Dónde encaja

```text
Pascal → Lexer → Parser/AST → SymbolTable → SemanticAnalyzer
       → AST decorado (Visitor) → TacGenerator → List<TacInstruction>
       → TacOptimizer → List<TacInstruction> optimizada
       → futuro generador NASM
```

Antes de optimizar, el compilador ya debió analizar semánticamente el código fuente. El optimizador **no ejecuta Pascal**; transforma una lista de instrucciones intermedias.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `src/IntermediateCode/TacOptimizer.java` | Nueva clase con todas las pasadas de optimización. |
| `test/IntermediateCode/TacOptimizerTest.java` | 98 comprobaciones de optimización y equivalencia. |
| `src/TestMain/Main.java` | Imprime TAC original y TAC optimizado. El programa de ejemplo comienza con una expresión constante para mostrar la diferencia. |
| `docs/hitos/H/README.md` | Este documento. |
| `README.md` | Agrega el enlace al Hito H. |

Se mantiene `TacInstruction.java` con cinco campos: `op`, `arg1`, `arg2`, `result` y `type`; los saltos y etiquetas tienen `type == null`.

## Cómo utilizar la clase

```java
TacGenerator generator = new TacGenerator();
List<TacInstruction> original = generator.generate(tree);

TacOptimizer optimizer = new TacOptimizer();
List<TacInstruction> optimized = optimizer.optimize(original);

System.out.println("TAC ORIGINAL");
for (TacInstruction instruction : original) {
    System.out.println(instruction);
}

System.out.println("TAC OPTIMIZADO");
for (TacInstruction instruction : optimized) {
    System.out.println(instruction);
}
```

`optimize(...)` crea otra lista y no modifica los cuádruplos originales. Las pasadas se repiten hasta que coinciden todos los campos de todas las instrucciones (punto fijo). No basta comprobar si cambió la **cantidad** de instrucciones: una constante propagada puede mejorar el TAC sin cambiar su longitud.

## 1. Plegado de constantes (*constant folding*)

Cuando conocemos ambos operandos en compilación, calculamos el resultado:

```text
Antes:                    Después:
_t1 = 2 * 3               x = 11
_t2 = _t1 + 5
x = _t2
```

Los temporales que ya no se necesitan se eliminan en una pasada posterior. La implementación utiliza `BigInteger` para calcular y **no sustituye** una operación de tipo `INTEGER` cuando su resultado saldría del rango de 32 bits. `REAL` se calcula con `double` solo si el resultado es finito. La división `/` es siempre `REAL`, según el contrato de Pascal Subset v1.0: `9 / 2` produce `4.5`, nunca `4`.

Se pueden plegar `NEG`, `I2R`, `+`, `-`, `*`, `/`, `==`, `!=`, `<`, `<=`, `>` y `>=`, cuando los operandos permiten un resultado seguro. Las expresiones booleanas se representan como `TRUE` y `FALSE`.

**División por cero:** nunca se pliega `x / 0`; si el resultado de una división podría provocar un error en ejecución, tampoco se elimina por estar en un temporal que aparentemente no se usa.

## 2. Propagación local de constantes

Se mantiene un `HashMap<String, String>` con las variables y temporales que tienen un valor constante conocido:

```text
x = 4                 x = 4
y = x                 y = 4
z = y + 1             z = 5
```

Cuando se vuelve a escribir `x`, se actualiza su entrada en el mapa. Así no se confunde el valor anterior con el nuevo.

**Límite deliberado:** el mapa se limpia al encontrar `LABEL`, `GOTO` o `IF_TRUE`. Por eso no se trasladan supuestas constantes a otra rama ni a la siguiente iteración de `WHILE`. Es una propagación **local y conservadora**, no un análisis global de constantes entre bloques básicos.

## 3. Simplificación algebraica segura

En operaciones `INTEGER`, se admiten reglas como:

| Expresión | Resultado |
|---|---|
| `x + 0`, `0 + x` | `x` |
| `x - 0` | `x` |
| `x * 1`, `1 * x` | `x` |
| `x * 0`, `0 * x` | `0` |

En operaciones `REAL` se admiten `x * 1.0`, `1.0 * x` y `x / 1.0`.

**No se aplican ciegamente las reglas enteras a los reales.** Por ejemplo, `x * 0.0` puede alterar el comportamiento de valores no finitos, y `x + 0.0` podría cambiar el signo de cero. No asumimos que todas las identidades de los números reales matemáticos son seguras para `double`.

## 4. Eliminación de código muerto

El optimizador identifica qué temporales `_t1`, `_t2`, etc. se necesitarán más adelante. Usa un análisis de **vivacidad hacia atrás** sobre el **grafo de flujo de control**: las sucesoras de una instrucción pueden estar después de ella, en una etiqueta de `GOTO` o en ambas rutas de `IF_TRUE`.

Las variables Pascal, como `x` o `total`, se **conservan** como resultados observables, aunque no aparezcan después en otra operación. Esto evita el defecto del ejemplo de optimización del profesor en el que se podría eliminar una variable final importante.

Si un temporal no llega a utilizarse, la operación que lo define puede suprimirse siempre que no elimine un posible error de ejecución. Se revisa el flujo real para que los usos en ciclos y ramas no se pierdan.

## 5. Saltos y código inalcanzable

- `IF_TRUE TRUE` se convierte en `GOTO`.
- `IF_TRUE FALSE` se elimina.
- Se elimina código sin camino desde la primera instrucción, calculando los sucesores de cada instrucción y sus etiquetas.
- Se eliminan `GOTO` a la etiqueta inmediatamente siguiente y etiquetas que ya no son referenciadas.
- **Los ciclos se conservan**: no se considera código muerto a una condición o un cuerpo por el simple hecho de estar detrás de un salto.

La simplificación del flujo de control puede mejorar una nueva pasada de plegado de constantes; por eso repetimos el proceso hasta llegar a un punto fijo.

## Métodos principales de `TacOptimizer`

| Método | Qué hace |
|---|---|
| `optimize()` | Coordina las pasadas hasta estabilizar el TAC. |
| `simplify()` | Propaga constantes y aplica plegado e identidades dentro de bloques. |
| `fold()` | Calcula operaciones entre literales válidos. |
| `algebraic()` | Detecta identidades seguras para `INTEGER` y `REAL`. |
| `remember()` | Actualiza el mapa de constantes cuando se escribe un destino. |
| `removeUnreachable()` | Recorre el grafo desde la primera instrucción y conserva lo alcanzable. |
| `successors()` | Identifica las rutas posibles de una instrucción. |
| `uses()` | Determina qué variables se leen. |
| `removeUnusedTemporaries()` | Calcula vivacidad para quitar temporales no utilizados. |
| `safeToRemove()` | Protege las divisiones de riesgo. |
| `removeRedundantJumps()` | Quita saltos inmediatos y etiquetas huérfanas. |
| `sameCode()` | Compara contenido, no solo tamaño, entre iteraciones. |

## Ejemplo integrado

En `Main.java` se emplea `counter := 2 * 3 - 6;` para mostrar la diferencia. Antes de optimizar, el generador produce temporales para multiplicación y resta. Después, la expresión se convierte en `counter = 0`. El `WHILE` conserva sus etiquetas, su condición y su salto de regreso, porque **no debe asumirse que `counter` sigue siendo 0 durante todas las iteraciones**.

Un programa aún más sencillo:

```pascal
PROGRAM Demo;
VAR
    x: INTEGER;
    r: REAL;
BEGIN
    x := 2 * 3 + 5;
    r := 9 / 2;
END.
```

Su TAC optimizado contiene asignaciones finales equivalentes a:

```text
x = 11
r = 4.5
```

## Pruebas en NetBeans

1. En el proyecto con **JDK 21**, realiza **Clean and Build**.
2. Ejecuta `TestMain.Main`: verás los apartados de TAC original y TAC optimizado.
3. Ejecuta `IntermediateCode.TacOptimizerTest` con **Run File**.
4. No necesitas JUnit: las pruebas son clases Java con método `main`.

El archivo de pruebas utiliza un pequeño intérprete **solo para verificar resultados**, no como parte del compilador. Compara las variables finales obtenidas a partir del TAC original y del TAC optimizado, incluidos ejemplos de `IF`, `WHILE`, `AND`, `OR`, `NOT` y valores `REAL`.

| Hito | Comprobaciones superadas |
|---|---:|
| B — Lexer | 104 |
| C — Parser | 140 |
| D — Tabla de símbolos | 38 |
| E — Análisis semántico | 70 |
| F — Visitor/AST decorado | 61 |
| G — Generador TAC | 74 |
| **H — Optimización TAC** | **98** |
| **Total acumulado** | **585** |

Se verificó compilación con Java 21 y la ejecución de las siete suites de prueba.

## Relación con los materiales del profesor

- `Optimización-TAC.txt` propone propagación de constantes, identidades algebraicas y eliminación de código muerto. Se aplican estas técnicas adaptadas a nuestro TAC tipado y al control de flujo completo.
- `Cuádruplo.pdf` presenta `Cuadruplo(op, arg1, arg2, res)` y pasadas de plegado, propagación y eliminación. Conservamos esta idea con `TacInstruction` (que agrega el tipo). Sus ejemplos trabajan sobre todo con enteros, mientras que nuestro lenguaje incorpora `REAL`, `BOOLEAN`, conversiones, comparaciones y saltos.
- En los ejemplos hay casos de eliminación de salidas finales y optimización de división que requieren más precauciones; por ello usamos vivacidad con flujo de control y mantenemos expresamente los resultados de las variables Pascal y divisiones potencialmente problemáticas.

## Límites y siguiente hito

La propagación de constantes es deliberadamente **local por bloque básico**; no implementamos un análisis global de constantes, una representación SSA ni optimizaciones agresivas de expresiones `REAL`. Tampoco realizamos todavía generación de ensamblador.

**Hito I:** traducir el TAC optimizado a **NASM x86-64**, distinguiendo operaciones enteras, reales, booleanas y saltos.

## Commit

`feat(optimizer): implementar optimización segura del TAC y sus pruebas`
