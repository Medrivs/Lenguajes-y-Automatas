package Parser;

// AssignmentNode: representa una asignación de variable.
public class AssignmentNode extends AstNode {
    private final String variableName;
    private final AstNode expression;

    // AssignmentNode (constructor): crea el nodo y guarda sus datos.
    public AssignmentNode(String variableName, AstNode expression, int line, int column) {
        super(line, column);
        this.variableName = variableName;
        this.expression = expression;
    }

    // getVariableName (consultar dato): devuelve el nombre de la variable.
    public String getVariableName() {
        return variableName;
    }

    // getExpression (consultar dato): devuelve la expresión asignada.
    public AstNode getExpression() {
        return expression;
    }

}
