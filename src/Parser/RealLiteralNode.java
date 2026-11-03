package Parser;

// RealLiteralNode: guarda un literal real sin convertirlo todavía.
public class RealLiteralNode extends AstNode {
    private final String lexeme;

    // RealLiteralNode (constructor): crea el nodo y guarda sus datos.
    public RealLiteralNode(String lexeme, int line, int column) {
        super(line, column);
        this.lexeme = lexeme;
    }

    // getLexeme (consultar dato): devuelve el número real como texto.
    public String getLexeme() {
        return lexeme;
    }

}
