# Hito E — Análisis semántico de Pascal Subset v1.0

## Objetivo

Después del Lexer, Parser y tabla de símbolos, revisar si el programa **tiene sentido**: variables declaradas, tipos correctos y uso de variables inicializadas.

## Archivos nuevos y modificados

- `src/Semantic/SemanticAnalyzer.java` — **nuevo**. Recorre las sentencias y expresiones del AST y comprueba sus reglas.
- `test/Semantic/SemanticTest.java` — **nuevo**. Pruebas sencillas ejecutables con `main`.
- `src/TestMain/Main.java` — **modificado**. Después de mostrar la tabla de símbolos, ejecuta el análisis semántico.

No cambiamos el Lexer, el Parser, el AST ni la tabla de símbolos.

## Relación con los materiales del profesor

1. `Analizador-Semantico.txt`: de allí adaptamos la idea central de recorrer el AST con métodos `visit...`, comprobar un identificador al encontrarlo y analizar primero la expresión de una asignación. **No copiamos el código Python**; usamos los nodos Java existentes.
2. `Tabla-de-Simbolos-con-Scope.txt`: adaptamos las comprobaciones de nombre declarado, inicialización y tipos. **No usamos `Stack` ni ámbitos múltiples** porque Pascal Subset v1.0 solo declara variables globales.
3. `Aportacion-sobre-Tabla-de-Simbolos.txt`: tomamos como referencia la separación entre registro de símbolos y verificación de sus usos. Funciones, parámetros, cadenas y valores de ejecución quedan fuera del lenguaje.

### ¿Por qué aún no usamos `accept(Visitor)`?

Para respetar el diseño del proyecto y conservar el código sencillo, este hito utiliza métodos `visit...()` en `SemanticAnalyzer` y un `if (node instanceof ...)` para elegir cuál llamar. **No es todavía el patrón Visitor formal**. En el Hito F podremos crear la interfaz `AstVisitor` y `accept()` sin mezclar el aprendizaje de ambos hitos.

## Orden de ejecución

1. `Parser.parseProgram()` construye un `ProgramNode`.
2. `new SymbolTable(program)` registra y valida declaraciones.
3. `new SemanticAnalyzer(table).analyze(program)` analiza las sentencias.
4. Si se encuentra un error, se lanza `RuntimeException` con línea, columna y motivo.
5. Solo si el análisis concluye sin errores podremos pasar a la futura generación TAC.

```java
Parser parser = new Parser(new Lexer(sourceCode));
ProgramNode program = parser.parseProgram();
SymbolTable table = new SymbolTable(program);

SemanticAnalyzer analyzer = new SemanticAnalyzer(table);
analyzer.analyze(program);
```

## Métodos del analizador semántico

| Método | Explicación breve |
|---|---|
| `analyze()` | Inicia el análisis del cuerpo del programa. |
| `visitBlock()` | Recorre las sentencias en orden. |
| `visitStatement()` | Decide si el nodo es asignación, `IF`, `WHILE` o bloque. |
| `visitAssignment()` | Busca la variable, revisa el tipo derecho y marca inicialización. |
| `visitIf()` | Exige condición `BOOLEAN`; analiza ambas ramas por separado. |
| `visitWhile()` | Exige condición `BOOLEAN`; analiza el cuerpo, pero no garantiza que se ejecute. |
| `visitExpression()` | Identifica el nodo expresión y devuelve su tipo. |
| `visitInteger()` | Rechaza literales mayores a `2147483647`. |
| `visitReal()` | Rechaza literales no finitos de `double`. |
| `visitIdentifier()` | Exige declaración e inicialización antes de leer una variable. |
| `visitUnary()` | Valida `NOT`, `+` y `-` unarios. |
| `visitBinary()` | Valida operaciones aritméticas, relacionales y booleanas. |
| `isNumeric()` | Comprueba `INTEGER` o `REAL`. |
| `normalizeName()` | Normaliza los nombres sin distinguir mayúsculas. |
| `error()` | Construye mensajes sencillos con línea y columna. |

## Reglas principales

| Expresión | Tipo resultado o regla |
|---|---|
| `INTEGER + INTEGER`, `*`, `-` | `INTEGER` |
| Operaciones aritméticas mezcladas `INTEGER/REAL` | `REAL` |
| División `/` de dos tipos numéricos | Siempre `REAL` |
| `<`, `<=`, `>`, `>=` numéricos | `BOOLEAN` |
| `=` y `<>` | Ambos numéricos, o ambos booleanos |
| `AND`, `OR`, `NOT` | Solo booleanos |
| `x := expr` | Tipos iguales o `INTEGER → REAL` |
| `IF`, `WHILE` | Condición obligatoriamente `BOOLEAN` |

### Variables inicializadas en distintas rutas

Para evitar declarar inicializada una variable que quizá nunca recibió un valor, utilizamos un `HashSet<String>` de nombres normalizados.

- **Asignación:** primero se analiza la expresión derecha; después se registra la variable como inicializada.
- **IF con ELSE:** al salir, solo se conservan las variables que estaban inicializadas en **ambas** ramas.
- **IF sin ELSE:** lo inicializado solo en `THEN` no se conserva fuera.
- **WHILE:** se revisa el cuerpo, pero al terminar se recupera el estado anterior; el ciclo puede ejecutarse cero veces.
- **AND y OR:** se revisan ambos operandos de manera conservadora. En ejecución, el código generado tendrá cortocircuito, pero el análisis no permite leer variables sin inicializar en ninguno de los operandos.

Ejemplo válido:

```pascal
PROGRAM Demo;
VAR
    x: INTEGER;
BEGIN
    IF TRUE THEN x := 1 ELSE x := 2;
    x := x + 1;
END.
```

Ejemplo inválido:

```pascal
PROGRAM Demo;
VAR
    x: INTEGER;
BEGIN
    IF TRUE THEN x := 1;
    x := x + 1;
END.
```

El segundo ejemplo falla porque **no hay ELSE** y no está garantizado que `x` se inicialice.

## Pruebas

Ejecutar desde NetBeans la clase `Semantic.SemanticTest` con **Run File**. Se comprobaron **70 casos** del Hito E. También se repitieron 104 comprobaciones del Lexer, 140 del Parser y 38 de la tabla de símbolos.

La clase `TestMain.Main` imprime ahora la sección **SEMANTIC ANALYSIS** si todo está correcto.

## Lo que no hacemos aún

- No se ejecutan instrucciones Pascal; únicamente se comprueban.
- No se generan valores de variables ni se ejecutan expresiones.
- No se hace una evaluación de constantes ni detección anticipada de división por cero (esa detección es opcional en el contrato).
- No se aplican múltiples ámbitos, funciones o parámetros.
- No implementamos el patrón Visitor formal ni la decoración del AST con tipos inferidos: se reservan para el Hito F.
- Se detiene en el **primer error semántico**, tal como permite el contrato.

## Commit recomendado

```text
feat(semantic): implementar el análisis semántico de Pascal Subset

- Recorrer el AST para comprobar declaraciones y lecturas de variables.
- Validar tipos de asignaciones y operadores aritméticos y booleanos.
- Exigir condiciones BOOLEAN en IF y WHILE.
- Controlar la inicialización de variables en ramas y ciclos.
- Revisar los rangos de literales INTEGER y REAL.
- Integrar el análisis con Main y agregar pruebas.
- Documentar el Hito E con comentarios sencillos en español.
```
