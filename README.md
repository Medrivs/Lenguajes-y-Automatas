# Compilador Pascal Subset v1.0

Proyecto académico de **Lenguajes y Autómatas** en **Java 21** y **Apache NetBeans**. El objetivo es crear un compilador para un subconjunto de Pascal, desde la gramática hasta el ensamblador NASM x86-64.

## Documentación de los hitos

| Hito | Tema | Documentación |
|---|---|---|
| A | Gramática Pascal Subset v1.0 | [README](docs/hitos/A/README.md) |
| B | Lexer y tokens | [README](docs/hitos/B/README.md) |
| C | Parser recursivo descendente y AST | [README](docs/hitos/C/README.md) |
| D | Tabla de símbolos global | [README](docs/hitos/D/README.md) |
| E | Análisis semántico | [README](docs/hitos/E/README.md) |
| F | Patrón Visitor y AST decorado | [README](docs/hitos/F/README.md) |
| G | Generación de código intermedio TAC | [README](docs/hitos/G/README.md) |
| H | Optimización de código intermedio TAC | [README](docs/hitos/H/README.md) |

**Hitos pendientes:** I (ensamblador), J (integración y pruebas).

## Estructura del proyecto

- `src/Grammar/`: gramática del lenguaje.
- `src/Lexer/`: tokens y análisis léxico.
- `src/Parser/`: parser y nodos del AST.
- `src/SymbolTable/`: variables declaradas y sus tipos.
- `src/Semantic/`: validación semántica y decoración.
- `src/Visitor/`: interfaz y visitantes del AST.
- `src/IntermediateCode/`: cuádruplos, generador y optimizador TAC.
- `src/TestMain/`: programa de ejemplo.
- `test/`: pruebas ejecutables de cada módulo.
- `docs/hitos/`: explicaciones por etapa.

## Ejecutar en NetBeans

1. Abre el proyecto y selecciona **JDK 21**.
2. Ejecuta **Clean and Build**.
3. Ejecuta `TestMain.Main` para ver tokens, AST, tabla de símbolos, validación, AST decorado y TAC.
4. Para probar cada módulo, ejecuta `Lexer.LexerTest`, `Parser.ParserTest`, `SymbolTable.SymbolTableTest`, `Semantic.SemanticTest`, `Visitor.VisitorTest` e `IntermediateCode.TacGeneratorTest`.

## Estilo de programación

- Nombres de clases, variables y métodos en **inglés**.
- Comentarios explicativos, errores y salidas en **español**.
- Java simple: `if`, `while`, `for`, clases, getters, listas, mapas y conjuntos.
- Mensaje de commit en español para cada cambio.

> Hito G genera código intermedio TAC, pero todavía no optimiza instrucciones, ejecuta programas Pascal ni emite ensamblador.
