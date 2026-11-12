# Compilador Pascal Subset v1.0

Proyecto académico de **Lenguajes y Autómatas** desarrollado en **Java 21** y **Apache NetBeans**. El compilador transforma un subconjunto de Pascal en código intermedio de tres direcciones (TAC), lo optimiza y genera **ensamblador NASM para Linux x86-64**.

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
| I | Generación de ensamblador NASM Linux x86-64 | [README](docs/hitos/I/README.md) |

**Hito pendiente:** J — Integración final y pruebas de extremo a extremo.

## Estructura del proyecto

- `src/Grammar/`: contrato y gramática EBNF.
- `src/Lexer/`: tokens y analizador léxico.
- `src/Parser/`: analizador sintáctico y nodos AST.
- `src/SymbolTable/`: registro de símbolos y tipos globales.
- `src/Semantic/`: verificación de tipos e inicializaciones.
- `src/Visitor/`: patrón Visitor y presentación del AST decorado.
- `src/IntermediateCode/`: generación y optimización de TAC.
- `src/ObjectCode/`: generación de ensamblador NASM x86-64.
- `src/TestMain/`: demostración de la cadena completa.
- `test/`: clases de prueba autónomas, sin JUnit.
- `docs/hitos/`: documentación por etapa.

## Ejecutar desde NetBeans

1. Abre el proyecto con **JDK 21**.
2. Ejecuta **Clean and Build**.
3. Ejecuta `TestMain.Main`: verás tokens, AST, símbolos, análisis semántico, AST decorado, TAC original, TAC optimizado y NASM.
4. El ensamblador se guarda automáticamente en **`build/programa.asm`**; no necesitas terminal para generarlo.
5. Ejecuta `ObjectCode.NasmGeneratorTest` y las suites anteriores con **Run File**.

Para ensamblar y ejecutar el archivo `.asm` necesitas **Linux x86-64 o WSL con NASM y `ld`**. El lenguaje no incluye impresión en pantalla, por lo que el ejecutable termina con código de salida `0` sin mostrar valores. Consulta [Hito I](docs/hitos/I/README.md).

## Convenciones de programación

- Clases, variables y métodos escritos en **inglés**.
- Comentarios breves, mensajes y documentación en **español**.
- Implementación Java sencilla: `if`, `switch`, `for`, listas, mapas y conjuntos.
- Cada cambio de código lleva un mensaje de commit en español.
