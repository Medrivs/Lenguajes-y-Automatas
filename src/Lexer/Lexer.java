package Lexer;

import java.util.Locale;

// Lexer (analizador léxico): lee el código fuente y lo divide en tokens.
public class Lexer {
    private final String input;    // Código fuente que vamos a analizar.
    private int position;          // Posición actual dentro del texto.
    private int line;              // Línea actual.
    private int column;            // Columna actual.
    private char currentChar;      // Carácter que estamos leyendo.

    // Lexer (constructor): recibe el código e inicia la lectura desde el principio.
    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.currentChar = input.length() > 0 ? input.charAt(0) : '\0';
    }

    // isAtEnd (fin de entrada): comprueba si ya leímos todos los caracteres.
    private boolean isAtEnd() {
        return position >= input.length();
    }

    // peek (mirar adelante): ve el siguiente carácter sin cambiar la posición.
    private char peek() {
        if (position + 1 < input.length()) {
            return input.charAt(position + 1);
        }
        return '\0';
    }

    // advance (avanzar): pasa al siguiente carácter y actualiza línea y columna.
    private void advance() {
        if (isAtEnd()) {
            return;
        }

        if (currentChar == '\r') {
            // En Windows, \r\n representa un solo salto de línea.
            if (peek() == '\n') {
                position++;
            }
            line++;
            column = 1;
        } else if (currentChar == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }

        position++;
        if (isAtEnd()) {
            currentChar = '\0';
        } else {
            currentChar = input.charAt(position);
        }
    }

    // skipWhitespace (saltar espacios): ignora espacios, tabulaciones y saltos de línea.
    private void skipWhitespace() {
        while (!isAtEnd() && (currentChar == ' ' || currentChar == '\t' ||
                currentChar == '\n' || currentChar == '\r')) {
            advance();
        }
    }

    // isDigit (es dígito): indica si el carácter está entre '0' y '9'.
    private boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    // isLetter (es letra): indica si es una letra inglesa de A a Z.
    private boolean isLetter(char character) {
        return (character >= 'a' && character <= 'z') ||
                (character >= 'A' && character <= 'Z');
    }

    // isIdentifierPart (parte de identificador): admite letras, números y '_'.
    private boolean isIdentifierPart(char character) {
        return isLetter(character) || isDigit(character) || character == '_';
    }

    // readNumber (leer número): reconoce números enteros y reales.
    // El punto pertenece al número solo cuando después viene un dígito.
    private Token readNumber() {
        int startLine = line;
        int startColumn = column;
        StringBuilder number = new StringBuilder();

        while (!isAtEnd() && isDigit(currentChar)) {
            number.append(currentChar);
            advance();
        }

        if (!isAtEnd() && currentChar == '.' && isDigit(peek())) {
            number.append('.');
            advance();

            while (!isAtEnd() && isDigit(currentChar)) {
                number.append(currentChar);
                advance();
            }

            return new Token(number.toString(), TokenType.FLOAT_LITERAL,
                    startLine, startColumn);
        }

        return new Token(number.toString(), TokenType.INT_LITERAL,
                startLine, startColumn);
    }

    // readIdentifier (leer identificador): reconoce nombres y palabras reservadas.
    // Para reconocer reservadas ignoramos mayúsculas, pero conservamos el texto original.
    private Token readIdentifier() {
        int startLine = line;
        int startColumn = column;
        StringBuilder name = new StringBuilder();

        while (!isAtEnd() && isIdentifierPart(currentChar)) {
            name.append(currentChar);
            advance();
        }

        String lexeme = name.toString();
        // Locale.ROOT hace que las mayúsculas no dependan del idioma del equipo.
        String keyword = lexeme.toUpperCase(Locale.ROOT);
        TokenType type;

        switch (keyword) {
            case "PROGRAM": type = TokenType.PROGRAM; break;
            case "VAR": type = TokenType.VAR; break;
            case "INTEGER": type = TokenType.INTEGER; break;
            case "REAL": type = TokenType.REAL; break;
            case "BOOLEAN": type = TokenType.BOOLEAN; break;
            case "BEGIN": type = TokenType.BEGIN; break;
            case "END": type = TokenType.END; break;
            case "IF": type = TokenType.IF; break;
            case "THEN": type = TokenType.THEN; break;
            case "ELSE": type = TokenType.ELSE; break;
            case "WHILE": type = TokenType.WHILE; break;
            case "DO": type = TokenType.DO; break;
            case "AND": type = TokenType.AND; break;
            case "OR": type = TokenType.OR; break;
            case "NOT": type = TokenType.NOT; break;
            case "TRUE": type = TokenType.TRUE; break;
            case "FALSE": type = TokenType.FALSE; break;
            default: type = TokenType.ID;
        }

        return new Token(lexeme, type, startLine, startColumn);
    }

    // nextToken (siguiente token): devuelve un token cada vez que el parser lo pide.
    // Primero salta espacios; luego reconoce números, identificadores y símbolos.
    public Token nextToken() {
        skipWhitespace();
        int startLine = line;
        int startColumn = column;

        if (isAtEnd()) {
            return new Token("", TokenType.EOF, startLine, startColumn);
        }

        if (isDigit(currentChar)) {
            return readNumber();
        }
        if (isLetter(currentChar)) {
            return readIdentifier();
        }

        switch (currentChar) {
            case '+':
                advance();
                return new Token("+", TokenType.SUM, startLine, startColumn);
            case '-':
                advance();
                return new Token("-", TokenType.SUB, startLine, startColumn);
            case '*':
                advance();
                return new Token("*", TokenType.MUL, startLine, startColumn);
            case '/':
                advance();
                return new Token("/", TokenType.DIV, startLine, startColumn);
            case '(':
                advance();
                return new Token("(", TokenType.LPARENT, startLine, startColumn);
            case ')':
                advance();
                return new Token(")", TokenType.RPARENT, startLine, startColumn);
            case ';':
                advance();
                return new Token(";", TokenType.SEMICOLON, startLine, startColumn);
            case '.':
                advance();
                return new Token(".", TokenType.DOT, startLine, startColumn);
            case ',':
                advance();
                return new Token(",", TokenType.COMMA, startLine, startColumn);
            case '=':
                advance();
                return new Token("=", TokenType.EQUAL, startLine, startColumn);

            case ':':
                advance();
                if (!isAtEnd() && currentChar == '=') {
                    advance();
                    return new Token(":=", TokenType.ASSIGN, startLine, startColumn);
                }
                return new Token(":", TokenType.COLON, startLine, startColumn);

            case '<':
                advance();
                if (!isAtEnd() && currentChar == '=') {
                    advance();
                    return new Token("<=", TokenType.LESS_EQUAL, startLine, startColumn);
                }
                if (!isAtEnd() && currentChar == '>') {
                    advance();
                    return new Token("<>", TokenType.NOT_EQUAL, startLine, startColumn);
                }
                return new Token("<", TokenType.LESS_THAN, startLine, startColumn);

            case '>':
                advance();
                if (!isAtEnd() && currentChar == '=') {
                    advance();
                    return new Token(">=", TokenType.GREATER_EQUAL, startLine, startColumn);
                }
                return new Token(">", TokenType.GREATER_THAN, startLine, startColumn);

            default:
                String character = String.valueOf(currentChar);
                if (currentChar == '\0') {
                    character = "\\0";
                }
                // Error léxico: un carácter no pertenece a nuestro lenguaje.
                throw new RuntimeException("Error léxico en la línea " + line +
                        ", columna " + column + ": carácter no válido '" + character + "'");
        }
    }
}
