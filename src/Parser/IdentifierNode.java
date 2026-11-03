package Parser;

// IdentifierNode: representa el uso de una variable.
public class IdentifierNode extends AstNode {
    private final String name;

    // IdentifierNode (constructor): crea el nodo y guarda sus datos.
    public IdentifierNode(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    // getName (consultar dato): devuelve el nombre original del identificador.
    public String getName() {
        return name;
    }

}
