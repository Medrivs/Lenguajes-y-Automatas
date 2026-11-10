package Parser;

import Visitor.AstVisitor;

// IntegerLiteralNode: guarda un literal entero sin convertirlo todavía.
public class IntegerLiteralNode extends AstNode {
    private final String lexeme;

    // IntegerLiteralNode (constructor): crea el nodo y guarda sus datos.
    public IntegerLiteralNode(String lexeme, int line, int column) {
        super(line, column);
        this.lexeme = lexeme;
    }

    // getLexeme (consultar dato): devuelve el número entero como texto.
    public String getLexeme() {
        return lexeme;
    }

    // accept (aceptar visitante): llama al método correspondiente del visitante.
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
