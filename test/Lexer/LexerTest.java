package Lexer;

// LexerTest (pruebas del lexer): verifica tokens, posiciones y errores sin usar JUnit.
public class LexerTest {
    private static int testCount = 0;

    // checkTokens (comprobar tokens): compara los tipos obtenidos con los esperados.
    private static void checkTokens(String source, TokenType... expectedTypes) {
        Lexer lexer = new Lexer(source);

        for (TokenType expectedType : expectedTypes) {
            Token token = lexer.nextToken();
            if (token.getType() != expectedType) {
                throw new RuntimeException("Se esperaba " + expectedType + " pero se encontró " + token);
            }
            testCount++;
        }

        if (lexer.nextToken().getType() != TokenType.EOF) {
            throw new RuntimeException("Se encontraron tokens adicionales inesperados: " + source);
        }
        testCount++;
    }

    // checkPosition (comprobar posición): revisa la línea y columna del primer token.
    private static void checkPosition(String source, int line, int column) {
        Token token = new Lexer(source).nextToken();
        if (token.getLine() != line || token.getColumn() != column) {
            throw new RuntimeException("Ubicación incorrecta del token: " + token);
        }
        testCount++;
    }

    // checkError (comprobar error): verifica que un carácter inválido reporte su posición.
    private static void checkError(String source, int line, int column) {
        Lexer lexer = new Lexer(source);
        boolean foundError = false;

        try {
            Token token = lexer.nextToken();
            while (token.getType() != TokenType.EOF) {
                token = lexer.nextToken();
            }
        } catch (RuntimeException error) {
            String location = "línea " + line + ", columna " + column;
            if (!error.getMessage().contains(location)) {
                throw new RuntimeException("Ubicación incorrecta del error: " + error.getMessage());
            }
            foundError = true;
        }

        if (!foundError) {
            throw new RuntimeException("Se esperaba un error léxico: " + source);
        }
        testCount++;
    }

    // main (inicio de las pruebas): ejecuta todos los casos y muestra el resultado.
    public static void main(String[] args) {
        String[] keywords = {"program", "var", "integer", "real", "boolean",
                "begin", "end", "if", "then", "else", "while", "do",
                "and", "or", "not", "true", "false"};
        TokenType[] types = {TokenType.PROGRAM, TokenType.VAR, TokenType.INTEGER,
                TokenType.REAL, TokenType.BOOLEAN, TokenType.BEGIN, TokenType.END,
                TokenType.IF, TokenType.THEN, TokenType.ELSE, TokenType.WHILE,
                TokenType.DO, TokenType.AND, TokenType.OR, TokenType.NOT,
                TokenType.TRUE, TokenType.FALSE};

        for (int i = 0; i < keywords.length; i++) {
            checkTokens(keywords[i], types[i]);
        }

        checkTokens("Counter counter_1", TokenType.ID, TokenType.ID);
        checkTokens("0 25 001.50 -3", TokenType.INT_LITERAL, TokenType.INT_LITERAL,
                TokenType.FLOAT_LITERAL, TokenType.SUB, TokenType.INT_LITERAL);
        checkTokens("12.", TokenType.INT_LITERAL, TokenType.DOT);
        checkTokens("12.25.", TokenType.FLOAT_LITERAL, TokenType.DOT);
        checkTokens(".5", TokenType.DOT, TokenType.INT_LITERAL); // El parser lo rechazará.
        checkTokens("1e3", TokenType.INT_LITERAL, TokenType.ID); // El parser lo rechazará.
        checkTokens("+ - * / ( ) ; . , :", TokenType.SUM, TokenType.SUB,
                TokenType.MUL, TokenType.DIV, TokenType.LPARENT,
                TokenType.RPARENT, TokenType.SEMICOLON, TokenType.DOT,
                TokenType.COMMA, TokenType.COLON);
        checkTokens("= := < > <= >= <>", TokenType.EQUAL, TokenType.ASSIGN,
                TokenType.LESS_THAN, TokenType.GREATER_THAN,
                TokenType.LESS_EQUAL, TokenType.GREATER_EQUAL, TokenType.NOT_EQUAL);
        checkTokens("PROGRAM Demo; VAR x: INTEGER; BEGIN x := 1; END.",
                TokenType.PROGRAM, TokenType.ID, TokenType.SEMICOLON,
                TokenType.VAR, TokenType.ID, TokenType.COLON, TokenType.INTEGER,
                TokenType.SEMICOLON, TokenType.BEGIN, TokenType.ID,
                TokenType.ASSIGN, TokenType.INT_LITERAL, TokenType.SEMICOLON,
                TokenType.END, TokenType.DOT);
        checkTokens("");
        checkTokens(" \t\r\n ");

        checkPosition("\r\n  age", 2, 3);
        checkPosition("\nx", 2, 1);
        checkPosition("\tA", 1, 2);

        Token token = new Lexer("bEgIn").nextToken();
        if (!token.getLexeme().equals("bEgIn") || token.getType() != TokenType.BEGIN) {
            throw new RuntimeException("Se perdió el texto original del token");
        }
        testCount++;

        if (!new Lexer("Variable").nextToken().getValue().equals("Variable")) {
            throw new RuntimeException("getValue() dejó de funcionar");
        }
        testCount++;

        checkError("_name", 1, 1);
        checkError("año", 1, 2);
        checkError("\"hello\"", 1, 1);
        checkError("a\0b", 1, 2);
        checkError("x\n$", 2, 1);
        checkError("{comment}", 1, 1);

        if (TokenType.values().length != 38) {
            throw new RuntimeException("Se esperaban 38 tipos de token");
        }
        testCount++;

        System.out.println("Correcto: " + testCount + " comprobaciones del lexer superadas.");
    }
}
