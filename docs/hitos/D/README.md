# Hito D — Tabla de símbolos

## Objetivo

Registrar las variables declaradas en el AST, su tipo y la ubicación de declaración. Pascal Subset v1.0 utiliza **un solo ámbito global**; no necesitamos pilas de ámbitos ni funciones.

## Archivos de la entrega del Hito D

| Archivo | Función |
|---|---|
| `src/SymbolTable/Symbol.java` | Representa una variable declarada |
| `src/SymbolTable/SymbolTable.java` | Guarda y permite buscar las variables |
| `test/SymbolTable/SymbolTableTest.java` | Pruebas de tabla y nombres duplicados |
| `src/TestMain/Main.java` | Muestra los símbolos después de construir el AST |

## ¿Qué guarda un Symbol?

`name` (nombre original), `type` (`INTEGER`, `REAL` o `BOOLEAN`), `line` y `column`.

La posición procede del nodo de declaración. Si una misma declaración contiene varios nombres, actualmente se utiliza la posición del primer identificador de esa declaración.

## Métodos de SymbolTable

- `SymbolTable(ProgramNode program)`: lee las declaraciones del AST y las registra.
- `normalizeName(String name)`: convierte a mayúsculas con `Locale.ROOT` para comparar sin distinguir el caso.
- `insert(...)`: registra la variable o informa error si está duplicada o coincide con el nombre del programa.
- `lookup(name)`: busca una variable; devuelve un `Symbol` o `null`.
- `contains(name)`: indica si una variable está registrada.
- `size()`: devuelve el número de variables.
- `getSymbols()`: devuelve una lista de símbolos. El orden no está garantizado, ya que se usa `HashMap`.

## Ejemplo

```pascal
PROGRAM Demo;
VAR
    Counter, total: INTEGER;
    ready: BOOLEAN;
BEGIN
    Counter := 1;
END.
```

Se registran **tres** variables. `lookup("counter")` y `lookup("COUNTER")` encuentran el mismo símbolo. `VAR Counter, COUNTER: INTEGER;` genera un error de declaración duplicada.

## Límites de este hito

Este módulo revisa **declaraciones**, no todos los usos de variables dentro de asignaciones o expresiones. Tampoco comprueba tipos de operaciones ni inicialización. La inicialización depende de las ramas `IF` y `WHILE`, por lo que se tratará en el análisis semántico del Hito E, no con un simple booleano global.

## Probar

Ejecuta `SymbolTable.SymbolTableTest` desde NetBeans. La entrega local del Hito D superó **38 comprobaciones**; además se ejecutaron sin fallos las 104 del Lexer y las 140 del Parser.

> Este README documenta el código de la entrega del Hito D. Si aún no aparecen las clases `src/SymbolTable/` en la rama, incorpora y confirma primero los archivos del proyecto actualizado.
