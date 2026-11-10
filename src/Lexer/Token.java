package Lexer;

// Token (elemento reconocido): guarda su texto, tipo y posición en el código.
public class Token {
    private final String lexeme;    // Texto original (lexema).
    private final TokenType type;    // Tipo de token.
    private final int line;          // Línea donde comienza.
    private final int column;        // Columna donde comienza.

    // Token (constructor): crea un token y guarda su ubicación.
    public Token(String lexeme, TokenType type, int line, int column) {
        this.lexeme = lexeme;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    // Token (constructor simple): permite crear tokens sin conocer su ubicación.
    public Token(String lexeme, TokenType type) {
        this(lexeme, type, 0, 0);
    }

    // getLexeme (obtener lexema): devuelve el texto original del token.
    public String getLexeme() {
        return lexeme;
    }

    // getType (obtener tipo): devuelve la categoría del token.
    public TokenType getType() {
        return type;
    }

    // getValue (obtener valor): devuelve el lexema para el parser actual.
    // En este hito no convertimos los números; el parser lo hará después.
    public String getValue() {
        return lexeme;
    }

    // getLine (obtener línea): devuelve la línea donde empieza el token.
    public int getLine() {
        return line;
    }

    // getColumn (obtener columna): devuelve la columna donde empieza el token.
    public int getColumn() {
        return column;
    }

    // toString (convertir a texto): muestra el token para revisar o depurar.
    @Override
    public String toString() {
        return type + " (" + lexeme + ") - línea " + line + ", columna " + column;
    }
}
