package Parser;

import Lexer.Lexer;
import Lexer.Token;
import Lexer.TokenType;
import java.util.ArrayList;
import java.util.List;

// Parser (analizador sintáctico): lee tokens y construye el árbol AST.
// Cada método sigue una regla de la gramática Pascal Subset v1.0.
public class Parser {
    private final Lexer lexer;
    private Token current;

    // Parser (constructor): recibe el lexer y lee el primer token.
    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    // match (coincidir): comprueba el token esperado y avanza al siguiente.
    private void match(TokenType expected) {
        if (current.getType() == expected) {
            current = lexer.nextToken();
        } else {
            throw syntaxError(expected.toString());
        }
    }

    // syntaxError (error sintáctico): muestra qué se esperaba y dónde ocurrió.
    private RuntimeException syntaxError(String expected) {
        return new RuntimeException("Syntax error at line " + current.getLine() +
                ", column " + current.getColumn() + ": expected " + expected +
                " but found " + current.getType() + " ('" + current.getLexeme() + "')");
    }

    // parseProgram (analizar programa): PROGRAM ID ; block . EOF
    // Construye la raíz con el nombre, las declaraciones y el cuerpo.
    public ProgramNode parseProgram() {
        Token programToken = current;
        match(TokenType.PROGRAM);

        String name = current.getLexeme();
        match(TokenType.ID);
        match(TokenType.SEMICOLON);

        // block -> declaration_part statement_part
        List<DeclarationNode> declarations = parseDeclarationPart();
        BlockNode body = parseCompoundStatement();

        match(TokenType.DOT);
        match(TokenType.EOF);
        return new ProgramNode(name, declarations, body,
                programToken.getLine(), programToken.getColumn());
    }

    // parseDeclarationPart (analizar declaraciones): VAR declaration ; ... o vacío.
    // Si aparece VAR, debe existir al menos una declaración.
    private List<DeclarationNode> parseDeclarationPart() {
        List<DeclarationNode> declarations = new ArrayList<>();

        if (current.getType() == TokenType.VAR) {
            match(TokenType.VAR);
            declarations.add(parseDeclaration());
            match(TokenType.SEMICOLON);

            while (current.getType() == TokenType.ID) {
                declarations.add(parseDeclaration());
                match(TokenType.SEMICOLON);
            }
        }
        return declarations;
    }

    // parseDeclaration (analizar declaración): identifier_list : type.
    private DeclarationNode parseDeclaration() {
        Token firstIdentifier = current;
        List<String> names = parseIdentifierList();
        match(TokenType.COLON);
        TokenType type = parseType();

        return new DeclarationNode(names, type,
                firstIdentifier.getLine(), firstIdentifier.getColumn());
    }

    // parseIdentifierList (analizar nombres): ID seguido de cero o más , ID.
    private List<String> parseIdentifierList() {
        List<String> names = new ArrayList<>();
        names.add(current.getLexeme());
        match(TokenType.ID);

        while (current.getType() == TokenType.COMMA) {
            match(TokenType.COMMA);
            names.add(current.getLexeme());
            match(TokenType.ID);
        }
        return names;
    }

    // parseType (analizar tipo): acepta INTEGER, REAL o BOOLEAN.
    private TokenType parseType() {
        TokenType type = current.getType();
        if (type == TokenType.INTEGER || type == TokenType.REAL ||
                type == TokenType.BOOLEAN) {
            match(type);
            return type;
        }
        throw syntaxError("INTEGER, REAL or BOOLEAN");
    }

    // parseCompoundStatement (analizar bloque): BEGIN statement_list END.
    // Permite bloques vacíos y un solo punto y coma antes de END.
    private BlockNode parseCompoundStatement() {
        Token beginToken = current;
        match(TokenType.BEGIN);
        BlockNode block = new BlockNode(beginToken.getLine(), beginToken.getColumn());

        if (current.getType() != TokenType.END) {
            block.addStatement(parseStatement());

            while (current.getType() == TokenType.SEMICOLON) {
                match(TokenType.SEMICOLON);
                if (current.getType() == TokenType.END) {
                    break;
                }
                block.addStatement(parseStatement());
            }
        }

        match(TokenType.END);
        return block;
    }

    // parseStatement (analizar sentencia): elige asignación, IF, WHILE o bloque.
    private AstNode parseStatement() {
        if (current.getType() == TokenType.ID) {
            return parseAssignment();
        } else if (current.getType() == TokenType.IF) {
            return parseIfStatement();
        } else if (current.getType() == TokenType.WHILE) {
            return parseWhileStatement();
        } else if (current.getType() == TokenType.BEGIN) {
            return parseCompoundStatement();
        }
        throw syntaxError("ID, IF, WHILE or BEGIN");
    }

    // parseAssignment (analizar asignación): ID := expression.
    private AssignmentNode parseAssignment() {
        Token identifier = current;
        String name = current.getLexeme();
        match(TokenType.ID);
        match(TokenType.ASSIGN);
        AstNode expression = parseExpression();

        return new AssignmentNode(name, expression,
                identifier.getLine(), identifier.getColumn());
    }

    // parseIfStatement (analizar IF): IF expression THEN statement [ELSE statement].
    // El ELSE se une al IF pendiente más cercano.
    private IfNode parseIfStatement() {
        Token ifToken = current;
        match(TokenType.IF);
        AstNode condition = parseExpression();
        match(TokenType.THEN);
        AstNode thenBranch = parseStatement();

        AstNode elseBranch = null;
        if (current.getType() == TokenType.ELSE) {
            match(TokenType.ELSE);
            elseBranch = parseStatement();
        }

        return new IfNode(condition, thenBranch, elseBranch,
                ifToken.getLine(), ifToken.getColumn());
    }

    // parseWhileStatement (analizar WHILE): WHILE expression DO statement.
    private WhileNode parseWhileStatement() {
        Token whileToken = current;
        match(TokenType.WHILE);
        AstNode condition = parseExpression();
        match(TokenType.DO);
        AstNode body = parseStatement();

        return new WhileNode(condition, body,
                whileToken.getLine(), whileToken.getColumn());
    }

    // parseExpression (analizar expresión): simple_expression [relop simple_expression].
    // Una expresión solo admite una comparación a este nivel.
    private AstNode parseExpression() {
        AstNode left = parseSimpleExpression();
        TokenType type = current.getType();

        if (type == TokenType.EQUAL || type == TokenType.NOT_EQUAL ||
                type == TokenType.LESS_THAN || type == TokenType.LESS_EQUAL ||
                type == TokenType.GREATER_THAN || type == TokenType.GREATER_EQUAL) {
            Token operator = current;
            match(type);
            AstNode right = parseSimpleExpression();
            left = new BinaryOperationNode(operator.getType(), left, right,
                    operator.getLine(), operator.getColumn());
        }
        return left;
    }

    // parseSimpleExpression (analizar suma): signo opcional, término y +, - u OR.
    // El signo inicial afecta al primer término completo, como indica la EBNF.
    private AstNode parseSimpleExpression() {
        Token sign = null;
        if (current.getType() == TokenType.SUM || current.getType() == TokenType.SUB) {
            sign = current;
            match(current.getType());
        }

        AstNode left = parseTerm();
        if (sign != null) {
            left = new UnaryOperationNode(sign.getType(), left,
                    sign.getLine(), sign.getColumn());
        }

        while (current.getType() == TokenType.SUM ||
                current.getType() == TokenType.SUB ||
                current.getType() == TokenType.OR) {
            Token operator = current;
            match(operator.getType());
            AstNode right = parseTerm();
            left = new BinaryOperationNode(operator.getType(), left, right,
                    operator.getLine(), operator.getColumn());
        }
        return left;
    }

    // parseTerm (analizar término): factor seguido de *, / o AND.
    private AstNode parseTerm() {
        AstNode left = parseFactor();

        while (current.getType() == TokenType.MUL ||
                current.getType() == TokenType.DIV ||
                current.getType() == TokenType.AND) {
            Token operator = current;
            match(operator.getType());
            AstNode right = parseFactor();
            left = new BinaryOperationNode(operator.getType(), left, right,
                    operator.getLine(), operator.getColumn());
        }
        return left;
    }

    // parseFactor (analizar factor): ID, literal, paréntesis o NOT factor.
    private AstNode parseFactor() {
        Token token = current;
        TokenType type = token.getType();

        if (type == TokenType.ID) {
            match(TokenType.ID);
            return new IdentifierNode(token.getLexeme(), token.getLine(), token.getColumn());
        } else if (type == TokenType.INT_LITERAL) {
            match(TokenType.INT_LITERAL);
            return new IntegerLiteralNode(token.getLexeme(), token.getLine(), token.getColumn());
        } else if (type == TokenType.FLOAT_LITERAL) {
            match(TokenType.FLOAT_LITERAL);
            return new RealLiteralNode(token.getLexeme(), token.getLine(), token.getColumn());
        } else if (type == TokenType.TRUE || type == TokenType.FALSE) {
            match(type);
            return new BooleanLiteralNode(type == TokenType.TRUE,
                    token.getLine(), token.getColumn());
        } else if (type == TokenType.LPARENT) {
            match(TokenType.LPARENT);
            AstNode expression = parseExpression();
            match(TokenType.RPARENT);
            return expression;
        } else if (type == TokenType.NOT) {
            match(TokenType.NOT);
            AstNode operand = parseFactor();
            return new UnaryOperationNode(TokenType.NOT, operand,
                    token.getLine(), token.getColumn());
        }

        throw syntaxError("ID, INT_LITERAL, FLOAT_LITERAL, TRUE, FALSE, ( or NOT");
    }
}
