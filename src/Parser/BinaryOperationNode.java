package Parser;

import Visitor.AstVisitor;

import Lexer.TokenType;

// BinaryOperationNode: representa una operación con dos operandos.
public class BinaryOperationNode extends AstNode {
    private final TokenType operator;
    private final AstNode left;
    private final AstNode right;

    // BinaryOperationNode (constructor): crea el nodo y guarda sus datos.
    public BinaryOperationNode(TokenType operator, AstNode left, AstNode right, int line, int column) {
        super(line, column);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    // getOperator (consultar dato): devuelve el operador de dos operandos.
    public TokenType getOperator() {
        return operator;
    }

    // getLeft (consultar dato): devuelve el operando izquierdo.
    public AstNode getLeft() {
        return left;
    }

    // getRight (consultar dato): devuelve el operando derecho.
    public AstNode getRight() {
        return right;
    }

    // accept (aceptar visitante): llama al método correspondiente del visitante.
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
