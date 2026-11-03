package Parser;

import java.util.List;
import Lexer.TokenType;

// DeclarationNode: representa una declaración de variables.
public class DeclarationNode extends AstNode {
    private final List<String> names;
    private final TokenType declaredType;

    // DeclarationNode (constructor): crea el nodo y guarda sus datos.
    public DeclarationNode(List<String> names, TokenType declaredType, int line, int column) {
        super(line, column);
        this.names = names;
        this.declaredType = declaredType;
    }

    // getNames (consultar dato): devuelve los nombres de esta declaración.
    public List<String> getNames() {
        return names;
    }

    // getDeclaredType (consultar dato): devuelve el tipo declarado.
    public TokenType getDeclaredType() {
        return declaredType;
    }

}
