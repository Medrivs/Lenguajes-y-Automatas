# Compilador Pascal Subset v1.0

Proyecto académico de **Lenguajes y Autómatas**, desarrollado en Java 21 y Apache NetBeans. El objetivo es construir un compilador pequeño y completo, desde el análisis léxico hasta la generación de ensamblador NASM x86-64.

## Documentación por hito

| Hito | Contenido | README |
|---|---|---|
| A | Gramática y contrato del lenguaje | [Hito A](docs/hitos/A/README.md) |
| B | Lexer, Token y TokenType | [Hito B](docs/hitos/B/README.md) |
| C | Parser recursivo descendente y AST | [Hito C](docs/hitos/C/README.md) |
| D | Tabla de símbolos global | [Hito D](docs/hitos/D/README.md) |

Los hitos posteriores contemplan análisis semántico, Visitor/AST decorado, código intermedio de tres direcciones (TAC), optimización, ensamblador y pruebas de integración.

## Reglas de programación

- Clases, variables y métodos en **inglés**.
- Comentarios breves en **español**, explicando la función de cada método.
- Implementación Java sencilla: `if`, `switch`, `while`, `for`, listas y mapas.
- Un mensaje de commit en español después de cada cambio de código.

## Estructura

- `src/Grammar/`: gramática EBNF.
- `src/Lexer/`: análisis léxico.
- `src/Parser/`: análisis sintáctico y AST.
- `src/SymbolTable/`: símbolos y tabla global (Hito D, al incorporar su código).
- `src/TestMain/`: ejemplo de ejecución.
- `test/`: pruebas sin JUnit.
- `docs/hitos/`: README explicativo de cada etapa.

## Ejecutar

Abre el proyecto en NetBeans, selecciona **JDK 21**, ejecuta **Clean and Build** y después `TestMain.Main`. Las clases `LexerTest`, `ParserTest` y `SymbolTableTest` (cuando se incorpore el Hito D) tienen su propio método `main` para verificar cada módulo.

> La tabla de símbolos no sustituye todavía el análisis semántico completo. La revisión de usos, tipos e inicialización de variables corresponde al siguiente hito.
