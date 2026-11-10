# Hito F — Patrón Visitor y AST decorado

## Objetivo

Preparar el AST para que distintas fases del compilador lo recorran sin cambiar el Parser. El análisis semántico ahora **decora** los nodos de expresión con sus tipos y enlaza cada referencia de variable con su símbolo. La generación de TAC pertenece al Hito G.

## ¿Qué es el patrón Visitor?

Cada nodo tiene un método `accept(visitor)` y llama al método `visit(...)` correspondiente a su clase:

```java
// En IntegerLiteralNode:
@Override
public void accept(AstVisitor visitor) {
    visitor.visit(this);
}
```

`AstVisitor` declara una sobrecarga `visit(...)` para cada uno de los **12 tipos concretos de nodo**. Así podemos emplear el mismo AST con distintos visitantes: `SemanticAnalyzer` (tipos) y `AstPrinterVisitor` (impresión), sin volver a analizar el código Pascal.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `src/Visitor/AstVisitor.java` | Interfaz de visitas a nodos |
| `src/Visitor/AstPrinterVisitor.java` | Imprime el árbol y sus tipos |
| `src/Parser/AstNode.java` | Guarda `inferredType` y declara `accept(...)` |
| `src/Parser/*Node.java` | Cada uno implementa `accept(...)` |
| `src/Parser/IdentifierNode.java` | `getSymbol()` identifica la declaración utilizada |
| `src/Parser/AssignmentNode.java` | `getTargetSymbol()` identifica la variable destino |
| `src/Semantic/SemanticAnalyzer.java` | Implementa `AstVisitor` y decora el AST |
| `src/TestMain/Main.java` | Imprime el AST decorado tras validar el programa |
| `test/Visitor/VisitorTest.java` | Pruebas nuevas del patrón y de las decoraciones |

## Decoración del AST

### Tipos calculados

Todos los nodos heredan `getInferredType()` y `setInferredType(...)` de `AstNode`. **Solo las expresiones reciben un tipo** (`INTEGER`, `REAL` o `BOOLEAN`). Antes del análisis semántico su valor es `null`; las sentencias siguen con `null`.

Ejemplo:

```pascal
r := 2 + 3.5;
```

Después de ejecutar el semántico, el segundo Visitor puede imprimir:

```text
Asignación a r : REAL
  Operación SUM : REAL
    Entero 2 : INTEGER
    Real 3.5 : REAL
```

El árbol no calcula el resultado: almacena su **tipo**, útil para insertar conversiones en el futuro TAC.

### Símbolos enlazados

`IdentifierNode.getSymbol()` y `AssignmentNode.getTargetSymbol()` devuelven la misma instancia de `Symbol` que la tabla encontró con `lookup()`. Los nombres se comparan sin distinguir mayúsculas/minúsculas.

## ¿Cómo se usa?

```java
SemanticAnalyzer analyzer = new SemanticAnalyzer(table);
analyzer.analyze(tree);

AstPrinterVisitor printer = new AstPrinterVisitor();
tree.accept(printer);
System.out.println(printer.getText());
```

El analizador comienza mediante `program.accept(this)`, y las reglas de tipos, inicialización, `IF` y `WHILE` siguen siendo las mismas del Hito E.

## Pruebas

| Suite | Comprobaciones correctas |
|---|---:|
| Lexer | 104 |
| Parser | 140 |
| Tabla de símbolos | 38 |
| Analizador semántico | 70 |
| Visitor y AST decorado | 61 |
| **Total** | **413** |

Se validó la compilación con Java 21 y Ant. Ejecuta `Visitor.VisitorTest` desde NetBeans; es una clase con `main`, sin JUnit.

## Nuevos materiales del profesor

`TAC-Generator.txt`, `Optimización-TAC.txt` y `Cuádruplo.pdf` muestran representaciones de tres direcciones y optimización. **Son útiles para el Hito G/H**, pero el Hito F no incorpora TAC ni optimizadores. Los ejemplos del profesor trabajan principalmente con enteros, mientras que nuestro contrato exige `INTEGER`, `REAL`, `BOOLEAN`, cortocircuito y división `/` con resultado `REAL`. Tendremos que adaptar esas ideas antes de generar código real.

## Límites

- `inferredType` representa un tipo, no un valor ejecutado.
- `Visitor` no sustituye las validaciones de `SymbolTable` ni altera la gramática.
- El analizador conserva el caso límite pendiente de `-2147483648` de los hitos anteriores.
- Los identificadores y destinos enlazados solo se consideran confiables **después** de pasar el análisis semántico.
- Hito G: crear un Visitor que genere TAC usando estos datos.

**Convención:** nombres de código en inglés; comentarios, mensajes y documentación en español.
