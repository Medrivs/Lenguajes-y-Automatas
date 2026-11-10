package Visitor;

import Parser.*;

// AstPrinterVisitor (impresor del AST): muestra el árbol y sus tipos calculados.
// Sirve para verificar el patrón Visitor sin generar código intermedio todavía.
public class AstPrinterVisitor implements AstVisitor {
    private final StringBuilder text;
    private int level;

    // AstPrinterVisitor (constructor): prepara el texto y la sangría.
    public AstPrinterVisitor() {
        text = new StringBuilder();
        level = 0;
    }

    // addLine (agregar línea): escribe un renglón con su nivel de sangría.
    private void addLine(String message) {
        for (int i = 0; i < level; i++) {
            text.append("  ");
        }
        text.append(message).append("\n");
    }

    // showType (mostrar tipo): añade el tipo si ya se analizó la expresión.
    private String showType(AstNode node) {
        if (node.getInferredType() == null) {
            return "";
        }
        return " : " + node.getInferredType();
    }

    // getText (obtener texto): devuelve la representación del árbol.
    public String getText() {
        return text.toString();
    }

    // visit (visitar programa): imprime su nombre y sus componentes.
    @Override
    public void visit(ProgramNode node) {
        addLine("Programa " + node.getName());
        level++;
        for (DeclarationNode declaration : node.getDeclarations()) {
            declaration.accept(this);
        }
        node.getBody().accept(this);
        level--;
    }

    // visit (visitar declaración): imprime las variables y su tipo declarado.
    @Override
    public void visit(DeclarationNode node) {
        addLine("Declaración " + String.join(", ", node.getNames()) + " : " + node.getDeclaredType());
    }

    // visit (visitar bloque): imprime todas las sentencias en orden.
    @Override
    public void visit(BlockNode node) {
        addLine("Bloque");
        level++;
        for (AstNode statement : node.getStatements()) {
            statement.accept(this);
        }
        level--;
    }

    // visit (visitar asignación): imprime el destino y su expresión.
    @Override
    public void visit(AssignmentNode node) {
        String type = "";
        if (node.getTargetSymbol() != null) {
            type = " : " + node.getTargetSymbol().getType();
        }
        addLine("Asignación a " + node.getVariableName() + type);
        level++;
        node.getExpression().accept(this);
        level--;
    }

    // visit (visitar IF): imprime condición y ramas THEN/ELSE.
    @Override
    public void visit(IfNode node) {
        addLine("IF");
        level++;
        addLine("Condición:");
        level++;
        node.getCondition().accept(this);
        level--;
        addLine("THEN:");
        level++;
        node.getThenBranch().accept(this);
        level--;
        if (node.getElseBranch() != null) {
            addLine("ELSE:");
            level++;
            node.getElseBranch().accept(this);
            level--;
        }
        level--;
    }

    // visit (visitar WHILE): imprime la condición y el cuerpo del ciclo.
    @Override
    public void visit(WhileNode node) {
        addLine("WHILE");
        level++;
        addLine("Condición:");
        level++;
        node.getCondition().accept(this);
        level--;
        addLine("Cuerpo:");
        level++;
        node.getBody().accept(this);
        level -= 2;
    }

    // visit (visitar binario): imprime operador, tipo y los dos operandos.
    @Override
    public void visit(BinaryOperationNode node) {
        addLine("Operación " + node.getOperator() + showType(node));
        level++;
        node.getLeft().accept(this);
        node.getRight().accept(this);
        level--;
    }

    // visit (visitar unario): imprime operador, tipo y operando.
    @Override
    public void visit(UnaryOperationNode node) {
        addLine("Operación unaria " + node.getOperator() + showType(node));
        level++;
        node.getOperand().accept(this);
        level--;
    }

    // visit (visitar identificador): muestra el nombre y tipo de la variable.
    @Override
    public void visit(IdentifierNode node) {
        addLine("Identificador " + node.getName() + showType(node));
    }

    // visit (visitar entero): muestra el valor y su tipo.
    @Override
    public void visit(IntegerLiteralNode node) {
        addLine("Entero " + node.getLexeme() + showType(node));
    }

    // visit (visitar real): muestra el valor y su tipo.
    @Override
    public void visit(RealLiteralNode node) {
        addLine("Real " + node.getLexeme() + showType(node));
    }

    // visit (visitar booleano): muestra TRUE o FALSE y su tipo.
    @Override
    public void visit(BooleanLiteralNode node) {
        if (node.getValue()) {
            addLine("Booleano TRUE" + showType(node));
        } else {
            addLine("Booleano FALSE" + showType(node));
        }
    }
}
