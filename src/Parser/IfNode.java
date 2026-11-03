package Parser;

// IfNode: representa una sentencia IF con ELSE opcional.
public class IfNode extends AstNode {
    private final AstNode condition;
    private final AstNode thenBranch;
    private final AstNode elseBranch;

    // IfNode (constructor): crea el nodo y guarda sus datos.
    public IfNode(AstNode condition, AstNode thenBranch, AstNode elseBranch, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    // getCondition (consultar dato): devuelve la condición del IF.
    public AstNode getCondition() {
        return condition;
    }

    // getThenBranch (consultar dato): devuelve la rama THEN.
    public AstNode getThenBranch() {
        return thenBranch;
    }

    // getElseBranch (consultar dato): devuelve la rama ELSE o null si no existe.
    public AstNode getElseBranch() {
        return elseBranch;
    }

}
