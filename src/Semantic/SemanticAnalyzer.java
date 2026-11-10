package Semantic;

import java.util.HashSet;
import java.util.Locale;
import Lexer.TokenType;
import Parser.AssignmentNode;
import Parser.AstNode;
import Parser.BinaryOperationNode;
import Parser.BlockNode;
import Parser.BooleanLiteralNode;
import Parser.IdentifierNode;
import Parser.IfNode;
import Parser.IntegerLiteralNode;
import Parser.ProgramNode;
import Parser.RealLiteralNode;
import Parser.UnaryOperationNode;
import Parser.WhileNode;
import SymbolTable.Symbol;
import SymbolTable.SymbolTable;

// SemanticAnalyzer (analizador semántico): revisa tipos y uso de variables del AST.
public class SemanticAnalyzer {
    private final SymbolTable table;
    private final HashSet<String> initialized;

    // SemanticAnalyzer (constructor): recibe la tabla de símbolos del programa.
    public SemanticAnalyzer(SymbolTable table) {
        this.table = table;
        this.initialized = new HashSet<>();
    }

    // analyze (analizar): recorre el programa después de construir el AST y la tabla.
    public void analyze(ProgramNode program) {
        initialized.clear();
        visitBlock(program.getBody());
    }

    // normalizeName (normalizar): compara variables sin distinguir mayúsculas.
    private String normalizeName(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    // error (error semántico): prepara un mensaje con línea y columna.
    private RuntimeException error(AstNode node, String message) {
        return new RuntimeException("Error semántico en la línea " + node.getLine()
                + ", columna " + node.getColumn() + ": " + message);
    }

    // visitBlock (visitar bloque): revisa las sentencias en el orden escrito.
    private void visitBlock(BlockNode block) {
        for (AstNode statement : block.getStatements()) {
            visitStatement(statement);
        }
    }

    // visitStatement (visitar sentencia): elige el análisis según el nodo recibido.
    private void visitStatement(AstNode statement) {
        if (statement instanceof AssignmentNode) {
            visitAssignment((AssignmentNode) statement);
        } else if (statement instanceof IfNode) {
            visitIf((IfNode) statement);
        } else if (statement instanceof WhileNode) {
            visitWhile((WhileNode) statement);
        } else if (statement instanceof BlockNode) {
            visitBlock((BlockNode) statement);
        } else {
            throw error(statement, "sentencia desconocida");
        }
    }

    // visitAssignment (visitar asignación): revisa variable, expresión y tipos.
    // Una variable se inicializa solo después de revisar el lado derecho.
    private void visitAssignment(AssignmentNode assignment) {
        Symbol variable = table.lookup(assignment.getVariableName());
        if (variable == null) {
            throw error(assignment, "la variable '" + assignment.getVariableName()
                    + "' no está declarada");
        }

        TokenType expressionType = visitExpression(assignment.getExpression());
        TokenType variableType = variable.getType();

        if (variableType != expressionType) {
            if (!(variableType == TokenType.REAL && expressionType == TokenType.INTEGER)) {
                throw error(assignment, "no se puede asignar un valor de tipo " + expressionType
                        + " a la variable '" + variable.getName() + "' de tipo " + variableType);
            }
        }

        initialized.add(normalizeName(variable.getName()));
    }

    // visitIf (visitar condición): exige BOOLEAN y analiza ambas rutas por separado.
    // Al terminar, solo conserva las variables inicializadas en ambas rutas.
    private void visitIf(IfNode node) {
        TokenType conditionType = visitExpression(node.getCondition());
        if (conditionType != TokenType.BOOLEAN) {
            throw error(node.getCondition(), "la condición de IF debe ser de tipo BOOLEAN");
        }

        HashSet<String> before = new HashSet<>(initialized);
        visitStatement(node.getThenBranch());
        HashSet<String> afterThen = new HashSet<>(initialized);

        initialized.clear();
        initialized.addAll(before);
        if (node.getElseBranch() != null) {
            visitStatement(node.getElseBranch());
        }

        afterThen.retainAll(initialized);
        initialized.clear();
        initialized.addAll(afterThen);
    }

    // visitWhile (visitar ciclo): exige BOOLEAN y comprueba el cuerpo.
    // No conserva asignaciones nuevas: el ciclo podría ejecutarse cero veces.
    private void visitWhile(WhileNode node) {
        TokenType conditionType = visitExpression(node.getCondition());
        if (conditionType != TokenType.BOOLEAN) {
            throw error(node.getCondition(), "la condición de WHILE debe ser de tipo BOOLEAN");
        }

        HashSet<String> before = new HashSet<>(initialized);
        visitStatement(node.getBody());
        initialized.clear();
        initialized.addAll(before);
    }

    // visitExpression (visitar expresión): comprueba una expresión y devuelve su tipo.
    private TokenType visitExpression(AstNode expression) {
        if (expression instanceof IntegerLiteralNode) {
            return visitInteger((IntegerLiteralNode) expression);
        }
        if (expression instanceof RealLiteralNode) {
            return visitReal((RealLiteralNode) expression);
        }
        if (expression instanceof BooleanLiteralNode) {
            return TokenType.BOOLEAN;
        }
        if (expression instanceof IdentifierNode) {
            return visitIdentifier((IdentifierNode) expression);
        }
        if (expression instanceof UnaryOperationNode) {
            return visitUnary((UnaryOperationNode) expression);
        }
        if (expression instanceof BinaryOperationNode) {
            return visitBinary((BinaryOperationNode) expression);
        }
        throw error(expression, "expresión desconocida");
    }

    // visitInteger (visitar entero): comprueba que el literal cabe en 32 bits.
    // Ignora ceros iniciales para aceptar valores como 00000000001.
    private TokenType visitInteger(IntegerLiteralNode node) {
        String digits = node.getLexeme();
        int start = 0;

        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
        }
        digits = digits.substring(start);

        if (digits.length() > 10) {
            throw error(node, "el literal INTEGER está fuera de rango: " + node.getLexeme());
        }
        if (digits.length() == 10 && digits.compareTo("2147483647") > 0) {
            throw error(node, "el literal INTEGER está fuera de rango: " + node.getLexeme());
        }
        return TokenType.INTEGER;
    }

    // visitReal (visitar real): comprueba que el literal es un REAL finito.
    private TokenType visitReal(RealLiteralNode node) {
        try {
            double value = Double.parseDouble(node.getLexeme());
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                throw error(node, "el literal REAL está fuera de rango: " + node.getLexeme());
            }
        } catch (NumberFormatException ex) {
            throw error(node, "el literal REAL está fuera de rango: " + node.getLexeme());
        }
        return TokenType.REAL;
    }

    // visitIdentifier (visitar variable): comprueba declaración e inicialización.
    private TokenType visitIdentifier(IdentifierNode node) {
        Symbol variable = table.lookup(node.getName());
        if (variable == null) {
            throw error(node, "la variable '" + node.getName() + "' no está declarada");
        }
        if (!initialized.contains(normalizeName(node.getName()))) {
            throw error(node, "la variable '" + node.getName() + "' no está inicializada");
        }
        return variable.getType();
    }

    // visitUnary (visitar operador unario): comprueba NOT y signos + o -.
    private TokenType visitUnary(UnaryOperationNode node) {
        TokenType type = visitExpression(node.getOperand());
        TokenType operator = node.getOperator();

        if (operator == TokenType.NOT) {
            if (type != TokenType.BOOLEAN) {
                throw error(node, "NOT requiere un operando de tipo BOOLEAN");
            }
            return TokenType.BOOLEAN;
        }
        if (operator == TokenType.SUM || operator == TokenType.SUB) {
            if (!isNumeric(type)) {
                throw error(node, "el operador unario + o - requiere un operando numérico");
            }
            return type;
        }
        throw error(node, "operador unario desconocido");
    }

    // isNumeric (es numérico): indica si el tipo es INTEGER o REAL.
    private boolean isNumeric(TokenType type) {
        return type == TokenType.INTEGER || type == TokenType.REAL;
    }

    // visitBinary (visitar operador binario): revisa ambos operandos y su resultado.
    // Se comprueban ambos lados de AND y OR aunque en ejecución haya cortocircuito.
    private TokenType visitBinary(BinaryOperationNode node) {
        TokenType left = visitExpression(node.getLeft());
        TokenType right = visitExpression(node.getRight());
        TokenType operator = node.getOperator();

        if (operator == TokenType.SUM || operator == TokenType.SUB || operator == TokenType.MUL) {
            if (!isNumeric(left) || !isNumeric(right)) {
                throw error(node, "el operador aritmético requiere operandos numéricos");
            }
            if (left == TokenType.REAL || right == TokenType.REAL) {
                return TokenType.REAL;
            }
            return TokenType.INTEGER;
        }
        if (operator == TokenType.DIV) {
            if (!isNumeric(left) || !isNumeric(right)) {
                throw error(node, "la división requiere operandos numéricos");
            }
            return TokenType.REAL;
        }
        if (operator == TokenType.AND || operator == TokenType.OR) {
            if (left != TokenType.BOOLEAN || right != TokenType.BOOLEAN) {
                throw error(node, "AND y OR requieren operandos de tipo BOOLEAN");
            }
            return TokenType.BOOLEAN;
        }
        if (operator == TokenType.EQUAL || operator == TokenType.NOT_EQUAL) {
            boolean twoNumbers = isNumeric(left) && isNumeric(right);
            boolean twoBooleans = left == TokenType.BOOLEAN && right == TokenType.BOOLEAN;
            if (!twoNumbers && !twoBooleans) {
                throw error(node, "= y <> requieren dos operandos numéricos o dos de tipo BOOLEAN");
            }
            return TokenType.BOOLEAN;
        }
        if (operator == TokenType.LESS_THAN || operator == TokenType.LESS_EQUAL ||
                operator == TokenType.GREATER_THAN || operator == TokenType.GREATER_EQUAL) {
            if (!isNumeric(left) || !isNumeric(right)) {
                throw error(node, "la comparación requiere operandos numéricos");
            }
            return TokenType.BOOLEAN;
        }
        throw error(node, "operador binario desconocido");
    }
}
