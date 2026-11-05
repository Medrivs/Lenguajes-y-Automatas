# Hito B — Analizador léxico (Lexer)

## Objetivo

Convertir el texto Pascal en una secuencia de **tokens**, identificando las palabras reservadas, los nombres, números y operadores antes del análisis sintáctico.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `src/Lexer/TokenType.java` | Enum con los 38 tipos de token |
| `src/Lexer/Token.java` | Guarda tipo, lexema original, línea y columna |
| `src/Lexer/Lexer.java` | Lee la fuente y devuelve tokens mediante `nextToken()` |
| `test/Lexer/LexerTest.java` | Pruebas léxicas sin JUnit |

## Métodos principales

- `isAtEnd()`: comprueba si ya terminó el texto.
- `peek()`: mira el carácter siguiente sin consumirlo.
- `advance()`: avanza y actualiza línea y columna.
- `skipWhitespace()`: omite espacios, tabulaciones y saltos de línea.
- `isDigit()`, `isLetter()` e `isIdentifierPart()`: verifican caracteres válidos según la gramática.
- `readNumber()`: distingue `INT_LITERAL`, `FLOAT_LITERAL` y el punto `DOT`.
- `readIdentifier()`: reconoce identificadores y palabras reservadas sin distinguir mayúsculas/minúsculas.
- `nextToken()`: reúne la lógica para obtener el siguiente token.

## Ejemplo

```pascal
counter := 12.5;
```

Produce los tokens `ID`, `ASSIGN`, `FLOAT_LITERAL` y `SEMICOLON`.

## Detalles importantes

- `12.` es un entero seguido del token `DOT`; `12.5` es un real.
- `<=`, `>=`, `<>` y `:=` son tokens únicos.
- La escritura original del identificador se conserva para mostrar errores.
- El lexer reporta caracteres inválidos con línea y columna.
- El lexer no revisa si una variable existe ni si una operación es válida por tipos.

## Pruebas

Ejecuta `Lexer.LexerTest` desde NetBeans. Se comprobaron **104 casos** en la entrega del Hito B.

## Convención de estilo

Código en inglés y comentarios breves en español. Se conservan métodos sencillos para facilitar la comprensión de cada paso.
