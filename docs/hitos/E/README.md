# Hito E — Análisis semántico

## Objetivo

Revisar que los programas reconocidos por Lexer y Parser también cumplan las **reglas de significado** de Pascal Subset v1.0. El analizador semántico utiliza el AST y la tabla de símbolos construidos en los hitos anteriores.

## Archivos nuevos

- `src/Semantic/SemanticAnalyzer.java`: revisa variables, expresiones, asignaciones, `IF` y `WHILE`.
- `test/Semantic/SemanticTest.java`: pruebas de programas semánticamente válidos e inválidos.
- `src/TestMain/Main.java`: incorpora la llamada al analizador semántico tras la construcción de la tabla.

> Los archivos de código se entregan por separado para que el autor los revise y haga su commit. Este README puede estar en GitHub antes de que se incorporen dichos archivos a `master`.

## Qué valida

| Regla | Ejemplo |
|---|---|
| Variable declarada | Error si `x := 1` y `x` no existe |
| Lectura inicializada | Error en `y := x + 1` si `x` no fue inicializada |
| Asignaciones | Se permite `INTEGER → REAL`, no `REAL → INTEGER` |
| Aritmética | `+`, `-`, `*` aceptan números; `/` devuelve `REAL` |
| Comparaciones | `<`, `<=`, `>`, `>=` requieren números |
| Igualdad | `=` y `<>` admiten dos números o dos booleanos |
| Lógica | `AND`, `OR`, `NOT` requieren booleanos |
| Condiciones | `IF` y `WHILE` exigen `BOOLEAN` |
| Literales | `INTEGER` de 32 bits; `REAL` de 64 bits finito |

## Inicialización y control de flujo

El analizador mantiene un `HashSet<String>` con los nombres de variables que están **definitivamente inicializadas**.

- **Asignación:** comprueba primero el lado derecho; después marca la variable de destino.
- **IF con ELSE:** tras ambas ramas, conserva solamente las variables inicializadas en las dos.
- **IF sin ELSE:** una asignación exclusiva de `THEN` no garantiza inicialización posterior.
- **WHILE:** comprueba las lecturas del cuerpo, pero no conserva inicializaciones nuevas al salir porque el cuerpo podría no ejecutarse.
- **AND/OR:** comprueba conservadoramente ambas expresiones aunque la futura ejecución genere cortocircuito.

### Ejemplo válido

```pascal
PROGRAM Demo;
VAR x: INTEGER;
BEGIN
    IF TRUE THEN x := 1 ELSE x := 2;
    x := x + 1;
END.
```

### Ejemplo inválido

```pascal
PROGRAM Demo;
VAR x: INTEGER;
BEGIN
    IF TRUE THEN x := 1;
    x := x + 1;
END.
```

El segundo falla: sin `ELSE`, `x` no está necesariamente inicializada al salir del `IF`.

## Métodos más importantes

| Método | Función |
|---|---|
| `analyze()` | Comienza el recorrido del programa |
| `visitBlock()` | Revisa sentencias en orden |
| `visitStatement()` | Identifica el tipo de sentencia |
| `visitAssignment()` | Comprueba declaración, tipo e inicialización |
| `visitIf()`, `visitWhile()` | Analizan el flujo de control |
| `visitExpression()` | Devuelve el tipo calculado de una expresión |
| `visitIdentifier()` | Comprueba declaración e inicialización |
| `visitUnary()`, `visitBinary()` | Revisan tipos de operadores |
| `visitInteger()`, `visitReal()` | Verifican los límites de los literales |

## Base en el material del profesor

Se utilizaron como referencia **`Analizador-Semantico.txt`**, **`Tabla-de-Simbolos-con-Scope.txt`** y **`Aportacion-sobre-Tabla-de-Simbolos.txt`**. Se adaptó la idea de recorrer nodos del AST y buscar variables; se omitieron las funciones, cadenas y ámbitos anidados porque no están en el contrato del lenguaje.

Los métodos `visit...` siguen la idea de un recorrido semántico. El **patrón Visitor formal** (`accept`, interfaz `AstVisitor`) y el AST decorado quedan reservados para el Hito F.

## Pruebas

- Hito E: **70 pruebas correctas**.
- Hito B: **104 comprobaciones** repetidas correctamente.
- Hito C: **140 comprobaciones** repetidas correctamente.
- Hito D: **38 comprobaciones** repetidas correctamente.
- Proyecto compilado correctamente con Java 21 y Ant.

Ejecuta `Semantic.SemanticTest` y después `TestMain.Main` desde NetBeans.

## Limitaciones deliberadas

El analizador no ejecuta programas ni evalúa valores en tiempo de compilación. Se detiene en el primer error semántico. La generación TAC, la optimización y el ensamblador vienen después.
