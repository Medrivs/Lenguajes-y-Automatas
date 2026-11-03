package Parser;

// BooleanLiteralNode: representa un literal TRUE o FALSE.
public class BooleanLiteralNode extends AstNode {
    private final boolean value;

    // BooleanLiteralNode (constructor): crea el nodo y guarda sus datos.
    public BooleanLiteralNode(boolean value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    // getValue (consultar dato): devuelve true o false.
    public boolean getValue() {
        return value;
    }

}
