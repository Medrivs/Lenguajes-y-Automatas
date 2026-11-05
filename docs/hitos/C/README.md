# Hito C — Parser recursivo descendente y AST

## Objetivo

Comprobar que los tokens siguen la gramática Pascal Subset v1.0 y construir un **AST** (árbol de sintaxis abstracta) que represente el programa sin ejecutarlo.

## Archivos

- `src/Parser/Parser.java`: métodos del parser.
- `src/Parser/ProgramNode.java`, `DeclarationNode.java` y `BlockNode.java`: estructura general.
- Nodos de sentencias: `AssignmentNode`, `IfNode` y `WhileNode`.
- Nodos de expresiones: `IdentifierNode`, `IntegerLiteralNode`, `RealLiteralNode`, `BooleanLiteralNode`, `UnaryOperationNode` y `BinaryOperationNode`.
- `test/Parser/ParserTest.java`: pruebas de sintaxis y estructura del árbol.

## Correspondencia entre gramática y métodos

| Método | Producción |
|---|---|
| `parseProgram()` | `program` y `block` |
| `parseDeclarationPart()`, `parseDeclaration()` | declaraciones |
| `parseIdentifierList()`, `parseType()` | nombres y tipos |
| `parseCompoundStatement()` | `BEGIN statement_list END` |
| `parseStatement()` | selección del tipo de sentencia |
| `parseAssignment()` | asignación |
| `parseIfStatement()`, `parseWhileStatement()` | control de flujo |
| `parseExpression()` | comparación |
| `parseSimpleExpression()` | suma, resta, `OR` y signo inicial |
| `parseTerm()` | multiplicación, división y `AND` |
| `parseFactor()` | identificadores, literales, paréntesis y `NOT` |

## Ejemplo de precedencia

```pascal
result := 2 + 3 * 4
```

El AST crea una suma cuyo operando derecho es una multiplicación. No calcula `14` durante el parsing: **solo conserva la estructura**.

## Reglas verificadas

Se admiten bloques vacíos, declaraciones múltiples y `ELSE` opcional. `parseProgram()` exige `EOF` después del punto final. Los errores sintácticos indican token esperado, token encontrado y posición.

## Qué no hace

El parser no comprueba variables duplicadas, referencias inexistentes, tipos incompatibles ni lecturas sin inicializar. Estas son responsabilidades posteriores.

## Pruebas

Ejecuta `Parser.ParserTest`. La entrega del Hito C superó **140 comprobaciones** y mantuvo las **104 del Lexer**.
