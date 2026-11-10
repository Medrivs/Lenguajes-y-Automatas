package Semantic;

import java.util.HashSet;
import java.util.Locale;
import Lexer.TokenType;
import Parser.*;
import SymbolTable.Symbol;
import SymbolTable.SymbolTable;
import Visitor.AstVisitor;

// SemanticAnalyzer (analizador semántico): visita el AST, comprueba tipos
// y guarda información útil en sus nodos para las siguientes fases.
public class SemanticAnalyzer implements AstVisitor {
    private final SymbolTable table;
    private final HashSet<String> initialized;

    // SemanticAnalyzer (constructor): recibe la tabla de símbolos del programa.
    public SemanticAnalyzer(SymbolTable table) {
        this.table = table;
        this.initialized = new HashSet<>();
    }

    // analyze (analizar): inicia el recorrido y reinicia las variables inicializadas.
    public void analyze(ProgramNode program) {
        initialized.clear();
        program.accept(this);
    }

    // normalizeName (normalizar): compara nombres sin distinguir mayúsculas.
    private String normalizeName(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    // error (error semántico): crea un mensaje con línea y columna.
    private RuntimeException error(AstNode node, String message) {
        return new RuntimeException("Error semántico en la línea " + node.getLine()
                + ", columna " + node.getColumn() + ": " + message);
    }

    // visitExpression (visitar expresión): usa accept y devuelve el tipo decorado.
    private TokenType visitExpression(AstNode expression) {
        expression.accept(this);
        if (expression.getInferredType() == null) {
            throw error(expression, "se esperaba una expresión");
        }
        return expression.getInferredType();
    }

    // visit (visitar programa): recorre las declaraciones y el bloque principal.
    @Override
    public void visit(ProgramNode node) {
        for (DeclarationNode declaration : node.getDeclarations()) {
            declaration.accept(this);
        }
        node.getBody().accept(this);
    }

    // visit (visitar declaración): las declaraciones ya están en SymbolTable.
    @Override
    public void visit(DeclarationNode node) {
        // No hay más comprobaciones por hacer en esta fase.
    }

    // visit (visitar bloque): procesa las sentencias una por una.
    @Override
    public void visit(BlockNode node) {
        for (AstNode statement : node.getStatements()) {
            statement.accept(this);
        }
    }

    // visit (visitar asignación): comprueba los tipos y enlaza la variable de destino.
    @Override
    public void visit(AssignmentNode node) {
        Symbol variable = table.lookup(node.getVariableName());
        if (variable == null) {
            throw error(node, "la variable '" + node.getVariableName() + "' no está declarada");
        }

        TokenType expressionType = visitExpression(node.getExpression());
        TokenType variableType = variable.getType();
        if (variableType != expressionType) {
            if (!(variableType == TokenType.REAL && expressionType == TokenType.INTEGER)) {
                throw error(node, "no se puede asignar un valor de tipo " + expressionType
                        + " a la variable '" + variable.getName() + "' de tipo " + variableType);
            }
        }

        node.setTargetSymbol(variable);
        initialized.add(normalizeName(variable.getName()));
    }

    // visit (visitar IF): exige BOOLEAN y conserva solo inicializaciones seguras.
    @Override
    public void visit(IfNode node) {
        TokenType conditionType = visitExpression(node.getCondition());
        if (conditionType != TokenType.BOOLEAN) {
            throw error(node.getCondition(), "la condición de IF debe ser de tipo BOOLEAN");
        }

        HashSet<String> before = new HashSet<>(initialized);
        node.getThenBranch().accept(this);
        HashSet<String> afterThen = new HashSet<>(initialized);

        initialized.clear();
        initialized.addAll(before);
        if (node.getElseBranch() != null) {
            node.getElseBranch().accept(this);
        }

        afterThen.retainAll(initialized);
        initialized.clear();
        initialized.addAll(afterThen);
    }

    // visit (visitar WHILE): revisa cuerpo y condición sin asegurar ejecución del ciclo.
    @Override
    public void visit(WhileNode node) {
        TokenType conditionType = visitExpression(node.getCondition());
        if (conditionType != TokenType.BOOLEAN) {
            throw error(node.getCondition(), "la condición de WHILE debe ser de tipo BOOLEAN");
        }
        HashSet<String> before = new HashSet<>(initialized);
        node.getBody().accept(this);
        initialized.clear();
        initialized.addAll(before);
    }

    // visit (visitar entero): valida el rango y decora con INTEGER.
    @Override
    public void visit(IntegerLiteralNode node) {
        String digits = node.getLexeme();
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
        }
        digits = digits.substring(start);
        if (digits.length() > 10 ||
                (digits.length() == 10 && digits.compareTo("2147483647") > 0)) {
            throw error(node, "el literal INTEGER está fuera de rango: " + node.getLexeme());
        }
        node.setInferredType(TokenType.INTEGER);
    }

    // visit (visitar real): valida el rango y decora con REAL.
    @Override
    public void visit(RealLiteralNode node) {
        try {
            double value = Double.parseDouble(node.getLexeme());
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                throw error(node, "el literal REAL está fuera de rango: " + node.getLexeme());
            }
        } catch (NumberFormatException ex) {
            throw error(node, "el literal REAL está fuera de rango: " + node.getLexeme());
        }
        node.setInferredType(TokenType.REAL);
    }

    // visit (visitar booleano): decora el literal con BOOLEAN.
    @Override
    public void visit(BooleanLiteralNode node) {
        node.setInferredType(TokenType.BOOLEAN);
    }

    // visit (visitar identificador): comprueba inicialización y enlaza su símbolo.
    @Override
    public void visit(IdentifierNode node) {
        Symbol variable = table.lookup(node.getName());
        if (variable == null) {
            throw error(node, "la variable '" + node.getName() + "' no está declarada");
        }
        if (!initialized.contains(normalizeName(node.getName()))) {
            throw error(node, "la variable '" + node.getName() + "' no está inicializada");
        }
        node.setSymbol(variable);
        node.setInferredType(variable.getType());
    }

    // isNumeric (es numérico): comprueba si el tipo es INTEGER o REAL.
    private boolean isNumeric(TokenType type) {
        return type == TokenType.INTEGER || type == TokenType.REAL;
    }

    // visit (visitar unario): revisa NOT, + y - y guarda el tipo resultante.
    @Override
    public void visit(UnaryOperationNode node) {
        TokenType type = visitExpression(node.getOperand());
        TokenType operator = node.getOperator();
        if (operator == TokenType.NOT) {
            if (type != TokenType.BOOLEAN) {
                throw error(node, "NOT requiere un operando de tipo BOOLEAN");
            }
            node.setInferredType(TokenType.BOOLEAN);
            return;
        }
        if (operator == TokenType.SUM || operator == TokenType.SUB) {
            if (!isNumeric(type)) {
                throw error(node, "el operador unario + o - requiere un operando numérico");
            }
            node.setInferredType(type);
            return;
        }
        throw error(node, "operador unario desconocido");
    }

    // visit (visitar binario): verifica ambos operandos y guarda el tipo final.
    @Override
    public void visit(BinaryOperationNode node) {
        TokenType left = visitExpression(node.getLeft());
        TokenType right = visitExpression(node.getRight());
        TokenType operator = node.getOperator();

        if (operator == TokenType.SUM || operator == TokenType.SUB || operator == TokenType.MUL) {
            if (!isNumeric(left) || !isNumeric(right)) {
                throw error(node, "el operador aritmético requiere operandos numéricos");
            }
            if (left == TokenType.REAL || right == TokenType.REAL) {
                node.setInferredType(TokenType.REAL);
            } else {
                node.setInferredType(TokenType.INTEGER);
            }
            return;
        }
        if (operator == TokenType.DIV) {
            if (!isNumeric(left) || !isNumeric(right)) {
                throw error(node, "la división requiere operandos numéricos");
            }
            node.setInferredType(TokenType.REAL);
            return;
        }
        if (operator == TokenType.AND || operator == TokenType.OR) {
            if (left != TokenType.BOOLEAN || right != TokenType.BOOLEAN) {
                throw error(node, "AND y OR requieren operandos de tipo BOOLEAN");
            }
            node.setInferredType(TokenType.BOOLEAN);
            return;
        }
        if (operator == TokenType.EQUAL || operator == TokenType.NOT_EQUAL) {
            boolean twoNumbers = isNumeric(left) && isNumeric(right);
            boolean twoBooleans = left == TokenType.BOOLEAN && right == TokenType.BOOLEAN;
            if (!twoNumbers && !twoBooleans) {
                throw error(node, "= y <> requieren dos operandos numéricos o dos de tipo BOOLEAN");
            }
            node.setInferredType(TokenType.BOOLEAN);
            return;
        }
        if (operator == TokenType.LESS_THAN || operator == TokenType.LESS_EQUAL ||
                operator == TokenType.GREATER_THAN || operator == TokenType.GREATER_EQUAL) {
            if (!isNumeric(left) || !isNumeric(right)) {
                throw error(node, "la comparación requiere operandos numéricos");
            }
            node.setInferredType(TokenType.BOOLEAN);
            return;
        }
        throw error(node, "operador binario desconocido");
    }
}
