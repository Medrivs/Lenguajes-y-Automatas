package Parser;

import Lexer.TokenType;

// UnaryOperationNode: representa un operador con un solo operando.
public class UnaryOperationNode extends AstNode {
    private final TokenType operator;
    private final AstNode operand;

    // UnaryOperationNode (constructor): crea el nodo y guarda sus datos.
    public UnaryOperationNode(TokenType operator, AstNode operand, int line, int column) {
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }

    // getOperator (consultar dato): devuelve el signo o NOT.
    public TokenType getOperator() {
        return operator;
    }

    // getOperand (consultar dato): devuelve la expresión afectada.
    public AstNode getOperand() {
        return operand;
    }

}
