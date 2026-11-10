# Hito G — Generación de código intermedio TAC (tres direcciones)

## Objetivo

Convertir el **AST decorado del Hito F** a instrucciones de código de tres direcciones (**TAC**, *Three Address Code*) representadas como **cuádruplos**. No ejecutamos Pascal ni generamos ensamblador todavía; el TAC será la entrada del optimizador (Hito H) y del generador NASM (Hito I).

## Orden de las fases

```text
Código Pascal
  → Lexer → Parser → AST
  → SymbolTable → SemanticAnalyzer (Visitor)
  → AST decorado (tipos y símbolos)
  → TacGenerator (Visitor)
  → List<TacInstruction>
```

**Importante:** primero debe terminar sin errores el análisis semántico. El generador utiliza `getInferredType()`, `getSymbol()` y `getTargetSymbol()`; no vuelve a comprobar las reglas del lenguaje.

## Archivos nuevos y modificados

| Archivo | Responsabilidad |
|---|---|
| `src/IntermediateCode/TacInstruction.java` | Representa cada cuádruplo (`op`, `arg1`, `arg2`, `result`) y conserva además el tipo del resultado (`type`). |
| `src/IntermediateCode/TacGenerator.java` | Implementa `AstVisitor`; crea temporales, etiquetas, conversiones y saltos. |
| `test/IntermediateCode/TacGeneratorTest.java` | 74 comprobaciones: estructura TAC, resultados simulados, tipos, cortocircuito y ciclos. |
| `src/TestMain/Main.java` | Muestra el TAC tras imprimir el AST decorado. |
| `docs/hitos/G/README.md` | Explica el diseño y cómo ejecutarlo. |
| `README.md` | Añade el enlace al Hito G. |

## ¿Qué es un cuádruplo?

Un cuádruplo describe una instrucción con cuatro campos: **operación, argumento 1, argumento 2 y resultado**. La clase `TacInstruction` añade `TokenType type` para distinguir, por ejemplo, una suma `INTEGER` de una `REAL`.

Ejemplo, para `x := 2 + 3 * 4;`:

```text
_t1 = 3 * 4
_t2 = 2 + _t1
x = _t2
```

Su representación interna es:

```java
new TacInstruction("*", "3", "4", "_t1", TokenType.INTEGER);
new TacInstruction("+", "2", "_t1", "_t2", TokenType.INTEGER);
new TacInstruction("ASSIGN", "_t2", null, "x", TokenType.INTEGER);
```

## Operaciones que emite el generador

| `op` | Representación | Descripción |
|---|---|---|
| `ASSIGN` | `x = _t1` | Asignación a variable o temporal. |
| `+`, `-`, `*`, `/` | `_t1 = a + b` | Aritmética con tipo asociado. |
| `NEG` | `_t1 = -a` | Signo negativo unario. |
| `I2R` | `_t1 = I2R a` | Conversión de `INTEGER` a `REAL`. |
| `==`, `!=`, `<`, `<=`, `>`, `>=` | `_t1 = a < b` | Comparaciones con resultado `BOOLEAN`. |
| `IF_TRUE` | `if b goto L1` | Salto condicional. |
| `GOTO` | `goto L2` | Salto incondicional. |
| `LABEL` | `L1:` | Marca una posición del programa. |

Las instrucciones `LABEL`, `GOTO` e `IF_TRUE` tienen `type == null`. No hay operadores TAC `AND`, `OR` o `NOT` porque se resuelven con **saltos y cortocircuito**.

## Clases y métodos importantes

### `TacInstruction.java`

- Constructor: recibe los cuatro campos del cuádruplo y el tipo opcional.
- `getOp()`, `getArg1()`, `getArg2()`, `getResult()`, `getType()`: permiten que un futuro optimizador o generador NASM trabaje sobre datos estructurados.
- `toString()`: convierte cada instrucción en una línea fácil de leer.

### `TacGenerator.java`

- `generate(program)`: reinicia contadores y genera instrucciones desde un `ProgramNode` previamente analizado.
- `newTemp()`: crea `_t1`, `_t2`, etc. El guion bajo inicial evita conflictos con identificadores Pascal, que deben empezar con una letra.
- `newLabel()`: crea `L1`, `L2`, etc.; las etiquetas pertenecen al flujo de control, no a la tabla de símbolos.
- `emit(...)`: añade instrucciones a la lista.
- `expressionValue(...)`: visita una expresión y devuelve el nombre donde queda su valor.
- `convertToReal(...)`: inserta la conversión explícita `I2R` cuando hace falta.
- `generateCondition(...)`: emite saltos según el resultado booleano, con cortocircuito.
- `materializeBoolean(...)`: produce un temporal `BOOLEAN` a partir de una expresión con saltos.
- `visit(AssignmentNode)`, `visit(IfNode)`, `visit(WhileNode)`: traducen sentencias a TAC.
- `visit(BinaryOperationNode)`, `visit(UnaryOperationNode)` y visitas de literales/identificadores: traducen expresiones.
- `getCode()`: devuelve una copia de la lista, evitando modificaciones accidentales.
- `getText()` y `writeFile(Path)`: muestran o guardan las instrucciones en un archivo UTF-8.

## Conversiones y tipos

Se mantiene el contrato del lenguaje:

- `INTEGER + INTEGER` produce `INTEGER` (también resta y multiplicación).
- Si un operando es `REAL`, la aritmética numérica produce `REAL`.
- **Toda división `/` produce `REAL`**, aun con dos enteros.
- Una asignación de `INTEGER` a `REAL` crea una conversión `I2R`.
- Una comparación numérica mixta convierte los operandos necesarios a `REAL` y devuelve `BOOLEAN`.
- `BOOLEAN` no se convierte a números.

Para `r := 9 / 2;` (con `r: REAL`):

```text
_t1 = I2R 9
_t2 = I2R 2
_t3 = _t1 / _t2
r = _t3
```

## Condicionales, ciclos y cortocircuito

### `IF ... THEN ... ELSE`

Para `IF x < 3 THEN x := 10 ELSE x := 20;` se producen saltos a las ramas y a la salida:

```text
_t1 = x < 3
if _t1 goto L1
goto L2
L1:
x = 10
goto L3
L2:
x = 20
L3:
```

### `WHILE`

Para `WHILE x < 3 DO x := x + 1;`:

```text
L1:
_t1 = x < 3
if _t1 goto L2
goto L3
L2:
_t2 = x + 1
x = _t2
goto L1
L3:
```

**La condición se genera después de `L1:`.** Por lo tanto se calcula de nuevo tras cada vuelta, no solo al entrar al ciclo.

### `AND`, `OR` y `NOT`

- `a AND b`: si `a` es falso, se omite `b`.
- `a OR b`: si `a` es verdadero, se omite `b`.
- `NOT a`: se intercambian los destinos verdadero/falso.
- Cuando una expresión booleana debe guardarse en una variable, `materializeBoolean` crea un temporal con `TRUE` o `FALSE` al que se llega por las rutas correspondientes.

Esto importa para expresiones como `FALSE AND (10 / 0 > 1)`: la rama derecha **no debe ejecutarse**. Las pruebas utilizan una simulación exclusiva del TAC para comprobar ese comportamiento.

## Ejecución en NetBeans

1. Abre el proyecto con **JDK 21**.
2. Ejecuta **Clean and Build**.
3. Ejecuta `TestMain.Main` para visualizar el nuevo apartado **CÓDIGO INTERMEDIO TAC**.
4. Ejecuta `IntermediateCode.TacGeneratorTest` mediante **Run File**.
5. Opcionalmente, desde un programa Java puedes llamar a `generator.writeFile(Path.of("salida.tac"));` para guardar el texto TAC.

Las pruebas continúan siendo clases `main` sin JUnit.

## Comprobaciones

| Módulo | Comprobaciones superadas |
|---|---:|
| Lexer (B) | 104 |
| Parser (C) | 140 |
| Tabla de símbolos (D) | 38 |
| Análisis semántico (E) | 70 |
| Visitor/AST decorado (F) | 61 |
| **Generador TAC (G)** | **74** |
| **Total** | **487** |

También se compiló correctamente con **Apache Ant y Java 21**.

## Relación con los materiales del profesor

- **`TAC-Generator.txt`**: adoptamos las ideas de contador de temporales, etiquetas y emisión de instrucciones. Adaptamos el ejemplo incompleto de `WHILE` para que vuelva a evaluar la condición y salte al inicio.
- **`Cuádruplo.pdf`**: utilizamos el formato `op, arg1, arg2, res` y una representación textual de asignaciones, operaciones y saltos. Añadimos el tipo del resultado porque este compilador maneja tres tipos.
- **`Optimización-TAC.txt`**: se conserva como referencia para el Hito H. No copiamos el optimizador todavía, porque sus reglas usan sobre todo enteros y no contemplan todo nuestro flujo de control.

## Alcance y límites

- Este hito **genera TAC, pero no ejecuta Pascal**. La simulación incluida en `TacGeneratorTest` se utiliza únicamente para comprobar los saltos.
- No realiza propagación de constantes ni eliminación de código muerto (Hito H).
- No genera ensamblador ni ejecutables NASM (Hito I).
- No añade funciones, arreglos, cadenas ni ámbitos nuevos al lenguaje.
- Conserva las reglas de tipos y las limitaciones conocidas del Hito E (por ejemplo, el caso límite del literal `-2147483648`).

## Commit

`feat(tac): implementar el generador de código intermedio con Visitor`
