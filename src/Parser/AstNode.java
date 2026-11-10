package Parser;

import Lexer.TokenType;
import Visitor.AstVisitor;

// AstNode (nodo del árbol): es la clase base de todos los nodos del AST.
// Guarda la ubicación para mostrar errores en las siguientes fases.
public abstract class AstNode {
    private final int line;
    private final int column;
    private TokenType inferredType;

    // AstNode (constructor): guarda la línea y columna del nodo.
    public AstNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    // getLine (obtener línea): devuelve la línea donde comienza el nodo.
    public int getLine() {
        return line;
    }

    // getColumn (obtener columna): devuelve la columna donde comienza el nodo.
    public int getColumn() {
        return column;
    }
    // getInferredType (obtener tipo): devuelve el tipo calculado por el semántico.
    // Es null antes del análisis o si el nodo no representa una expresión.
    public TokenType getInferredType() {
        return inferredType;
    }

    // setInferredType (guardar tipo): decora una expresión con su tipo.
    public void setInferredType(TokenType inferredType) {
        this.inferredType = inferredType;
    }

    // accept (aceptar visitante): deja que el visitante procese este nodo.
    public abstract void accept(AstVisitor visitor);
}

