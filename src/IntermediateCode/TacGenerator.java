package IntermediateCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Lexer.TokenType;
import Parser.*;
import Visitor.AstVisitor;

// TacGenerator (generador TAC): recorre el AST decorado con el patrón Visitor.
// Produce cuádruplos sin ejecutar todavía el programa Pascal.
public class TacGenerator implements AstVisitor {
    private final List<TacInstruction> code;
    private int tempCount;
    private int labelCount;
    private String lastValue;

    // TacGenerator (constructor): prepara los contadores y la lista de instrucciones.
    public TacGenerator() {
        code = new ArrayList<>();
        tempCount = 0;
        labelCount = 0;
    }

    // generate (generar): empieza desde el programa validado y reinicia los datos.
    // El análisis semántico debe ejecutarse antes para decorar tipos y símbolos.
    public List<TacInstruction> generate(ProgramNode program) {
        code.clear();
        tempCount = 0;
        labelCount = 0;
        lastValue = null;
        program.accept(this);
        return getCode();
    }

    // getCode (obtener código): devuelve una copia de las instrucciones generadas.
    public List<TacInstruction> getCode() {
        return new ArrayList<>(code);
    }

    // getText (obtener texto): une las instrucciones para mostrarlas en consola.
    public String getText() {
        StringBuilder text = new StringBuilder();
        for (TacInstruction instruction : code) {
            text.append(instruction).append("\n");
        }
        return text.toString();
    }

    // writeFile (guardar archivo): exporta las instrucciones a un archivo de texto.
    public void writeFile(Path path) throws IOException {
        Files.writeString(path, getText(), StandardCharsets.UTF_8);
    }

    // newTemp (nuevo temporal): crea nombres que no pueden ser variables Pascal.
    private String newTemp() {
        tempCount++;
        return "_t" + tempCount;
    }

    // newLabel (nueva etiqueta): crea destinos para saltos e instrucciones de control.
    private String newLabel() {
        labelCount++;
        return "L" + labelCount;
    }

    // emit (emitir): agrega un cuádruplo con su tipo opcional.
    private void emit(String op, String arg1, String arg2, String result, TokenType type) {
        code.add(new TacInstruction(op, arg1, arg2, result, type));
    }

    // emitLabel (etiquetar): escribe una etiqueta en la posición actual.
    private void emitLabel(String label) {
        emit("LABEL", null, null, label, null);
    }

    // emitGoto (saltar): envía el flujo a una etiqueta.
    private void emitGoto(String label) {
        emit("GOTO", null, null, label, null);
    }

    // requireType (revisar tipo): evita usar un AST sin análisis semántico.
    private TokenType requireType(AstNode node) {
        if (node.getInferredType() == null) {
            throw new IllegalStateException("Error TAC: primero debe realizarse el análisis semántico");
        }
        return node.getInferredType();
    }

    // expressionValue (valor de expresión): visita una expresión y toma su resultado.
    private String expressionValue(AstNode expression) {
        requireType(expression);
        expression.accept(this);
        return lastValue;
    }

    // convertToReal (convertir a real): agrega una conversión INTEGER a REAL.
    private String convertToReal(String value, TokenType type) {
        if (type != TokenType.INTEGER) {
            return value;
        }
        String temporary = newTemp();
        emit("I2R", value, null, temporary, TokenType.REAL);
        return temporary;
    }

    // materializeBoolean (materializar booleano): transforma saltos en un valor BOOLEAN.
    // Sirve para usar AND, OR y NOT en asignaciones y comparaciones.
    private String materializeBoolean(AstNode expression) {
        String trueLabel = newLabel();
        String falseLabel = newLabel();
        String endLabel = newLabel();
        String temporary = newTemp();

        generateCondition(expression, trueLabel, falseLabel);
        emitLabel(trueLabel);
        emit("ASSIGN", "TRUE", null, temporary, TokenType.BOOLEAN);
        emitGoto(endLabel);
        emitLabel(falseLabel);
        emit("ASSIGN", "FALSE", null, temporary, TokenType.BOOLEAN);
        emitLabel(endLabel);
        return temporary;
    }

    // generateCondition (generar condición): salta sin evaluar operandos innecesarios.
    // Esta rutina respeta el cortocircuito de AND, OR y NOT.
    private void generateCondition(AstNode expression, String trueLabel, String falseLabel) {
        if (requireType(expression) != TokenType.BOOLEAN) {
            throw new IllegalStateException("Error TAC: una condición debe ser BOOLEAN");
        }
        if (expression instanceof BinaryOperationNode) {
            BinaryOperationNode binary = (BinaryOperationNode) expression;
            if (binary.getOperator() == TokenType.AND) {
                String nextLabel = newLabel();
                generateCondition(binary.getLeft(), nextLabel, falseLabel);
                emitLabel(nextLabel);
                generateCondition(binary.getRight(), trueLabel, falseLabel);
                return;
            }
            if (binary.getOperator() == TokenType.OR) {
                String nextLabel = newLabel();
                generateCondition(binary.getLeft(), trueLabel, nextLabel);
                emitLabel(nextLabel);
                generateCondition(binary.getRight(), trueLabel, falseLabel);
                return;
            }
        }
        if (expression instanceof UnaryOperationNode) {
            UnaryOperationNode unary = (UnaryOperationNode) expression;
            if (unary.getOperator() == TokenType.NOT) {
                generateCondition(unary.getOperand(), falseLabel, trueLabel);
                return;
            }
        }
        if (expression instanceof BooleanLiteralNode) {
            BooleanLiteralNode literal = (BooleanLiteralNode) expression;
            emitGoto(literal.getValue() ? trueLabel : falseLabel);
            return;
        }
        String condition = expressionValue(expression);
        emit("IF_TRUE", condition, null, trueLabel, null);
        emitGoto(falseLabel);
    }

    // visit (visitar programa): genera código del bloque principal.
    @Override
    public void visit(ProgramNode node) {
        node.getBody().accept(this);
    }

    // visit (visitar declaración): la tabla de símbolos ya registró las variables.
    @Override
    public void visit(DeclarationNode node) {
        // Las declaraciones no producen instrucciones ejecutables.
    }

    // visit (visitar bloque): conserva el orden original de las sentencias.
    @Override
    public void visit(BlockNode node) {
        for (AstNode statement : node.getStatements()) {
            statement.accept(this);
        }
    }

    // visit (visitar asignación): evalúa el lado derecho y escribe en la variable.
    @Override
    public void visit(AssignmentNode node) {
        if (node.getTargetSymbol() == null) {
            throw new IllegalStateException("Error TAC: primero debe realizarse el análisis semántico (falta el símbolo de la asignación)");
        }
        String value = expressionValue(node.getExpression());
        TokenType sourceType = node.getExpression().getInferredType();
        TokenType destinationType = node.getTargetSymbol().getType();
        if (destinationType == TokenType.REAL) {
            value = convertToReal(value, sourceType);
        }
        emit("ASSIGN", value, null, node.getTargetSymbol().getName(), destinationType);
    }

    // visit (visitar IF): crea etiquetas para THEN, ELSE y la salida.
    @Override
    public void visit(IfNode node) {
        String thenLabel = newLabel();
        String elseLabel = newLabel();
        generateCondition(node.getCondition(), thenLabel, elseLabel);
        emitLabel(thenLabel);
        node.getThenBranch().accept(this);
        if (node.getElseBranch() != null) {
            String endLabel = newLabel();
            emitGoto(endLabel);
            emitLabel(elseLabel);
            node.getElseBranch().accept(this);
            emitLabel(endLabel);
        } else {
            emitLabel(elseLabel);
        }
    }

    // visit (visitar WHILE): coloca la condición después de la etiqueta inicial.
    // De este modo se vuelve a calcular la condición en cada iteración.
    @Override
    public void visit(WhileNode node) {
        String startLabel = newLabel();
        String bodyLabel = newLabel();
        String endLabel = newLabel();
        emitLabel(startLabel);
        generateCondition(node.getCondition(), bodyLabel, endLabel);
        emitLabel(bodyLabel);
        node.getBody().accept(this);
        emitGoto(startLabel);
        emitLabel(endLabel);
    }

    // visit (visitar entero): conserva el lexema sin alterar su valor.
    @Override
    public void visit(IntegerLiteralNode node) {
        requireType(node);
        lastValue = node.getLexeme();
    }

    // visit (visitar real): conserva el número escrito en el programa.
    @Override
    public void visit(RealLiteralNode node) {
        requireType(node);
        lastValue = node.getLexeme();
    }

    // visit (visitar booleano): representa constantes como TRUE o FALSE.
    @Override
    public void visit(BooleanLiteralNode node) {
        requireType(node);
        lastValue = node.getValue() ? "TRUE" : "FALSE";
    }

    // visit (visitar identificador): usa el nombre de su símbolo declarado.
    @Override
    public void visit(IdentifierNode node) {
        requireType(node);
        if (node.getSymbol() == null) {
            throw new IllegalStateException("Error TAC: primero debe realizarse el análisis semántico (falta el símbolo del identificador)");
        }
        lastValue = node.getSymbol().getName();
    }

    // visit (visitar unario): produce NEG, ignora + y usa saltos para NOT.
    @Override
    public void visit(UnaryOperationNode node) {
        TokenType type = requireType(node);
        TokenType operator = node.getOperator();
        if (operator == TokenType.NOT) {
            lastValue = materializeBoolean(node);
            return;
        }
        String value = expressionValue(node.getOperand());
        if (operator == TokenType.SUM) {
            lastValue = value;
            return;
        }
        if (operator == TokenType.SUB) {
            String temporary = newTemp();
            emit("NEG", value, null, temporary, type);
            lastValue = temporary;
            return;
        }
        throw new IllegalStateException("Error TAC: operador unario desconocido");
    }

    // visit (visitar binario): genera instrucciones para aritmética y comparaciones.
    @Override
    public void visit(BinaryOperationNode node) {
        TokenType resultType = requireType(node);
        TokenType operator = node.getOperator();
        if (operator == TokenType.AND || operator == TokenType.OR) {
            lastValue = materializeBoolean(node);
            return;
        }

        String left = expressionValue(node.getLeft());
        String right = expressionValue(node.getRight());
        TokenType leftType = node.getLeft().getInferredType();
        TokenType rightType = node.getRight().getInferredType();

        // Conversión explícita para operaciones reales o números de distinto tipo.
        boolean numeric = isNumeric(leftType) && isNumeric(rightType);
        boolean realOperands = numeric && (operator == TokenType.DIV ||
                leftType == TokenType.REAL || rightType == TokenType.REAL);
        if (realOperands) {
            left = convertToReal(left, leftType);
            right = convertToReal(right, rightType);
        }

        String op = operatorText(operator);
        String temporary = newTemp();
        emit(op, left, right, temporary, resultType);
        lastValue = temporary;
    }

    // isNumeric (es numérico): comprueba los tipos INTEGER y REAL.
    private boolean isNumeric(TokenType type) {
        return type == TokenType.INTEGER || type == TokenType.REAL;
    }

    // operatorText (convertir operador): traduce los tokens a símbolos del TAC.
    private String operatorText(TokenType operator) {
        switch (operator) {
            case SUM: return "+";
            case SUB: return "-";
            case MUL: return "*";
            case DIV: return "/";
            case EQUAL: return "==";
            case NOT_EQUAL: return "!=";
            case LESS_THAN: return "<";
            case LESS_EQUAL: return "<=";
            case GREATER_THAN: return ">";
            case GREATER_EQUAL: return ">=";
            default: throw new IllegalStateException("Error TAC: operador binario desconocido");
        }
    }
}
