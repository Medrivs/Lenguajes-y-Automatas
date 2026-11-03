package Parser;

// WhileNode: representa un ciclo WHILE.
public class WhileNode extends AstNode {
    private final AstNode condition;
    private final AstNode body;

    // WhileNode (constructor): crea el nodo y guarda sus datos.
    public WhileNode(AstNode condition, AstNode body, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }

    // getCondition (consultar dato): devuelve la condición del ciclo.
    public AstNode getCondition() {
        return condition;
    }

    // getBody (consultar dato): devuelve la sentencia que se repite.
    public AstNode getBody() {
        return body;
    }

}
