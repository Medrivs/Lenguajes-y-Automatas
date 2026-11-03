package Parser;

// AstNode (nodo del árbol): es la clase base de todos los nodos del AST.
// Guarda la ubicación para mostrar errores en las siguientes fases.
public abstract class AstNode {
    private final int line;
    private final int column;

    // AstNode (constructor): guarda la línea y columna del nodo.
    public AstNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    // getLine (obtener línea): devuelve la línea donde comienza el nodo.
    public int getLine() {
        return line;
    }

    // getColumn (obtener columna): devuelve la columna donde comienza el nodo.
    public int getColumn() {
        return column;
    }
}
