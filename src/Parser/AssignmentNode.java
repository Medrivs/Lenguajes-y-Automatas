package Parser;

import Visitor.AstVisitor;
import SymbolTable.Symbol;

// AssignmentNode: representa una asignación de variable.
public class AssignmentNode extends AstNode {
    private final String variableName;
    private Symbol targetSymbol;
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

    // getTargetSymbol (obtener destino): devuelve el símbolo de la variable asignada.
    public Symbol getTargetSymbol() {
        return targetSymbol;
    }

    // setTargetSymbol (guardar destino): enlaza la asignación con su variable.
    public void setTargetSymbol(Symbol targetSymbol) {
        this.targetSymbol = targetSymbol;
    }

    // accept (aceptar visitante): llama al método correspondiente del visitante.
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
