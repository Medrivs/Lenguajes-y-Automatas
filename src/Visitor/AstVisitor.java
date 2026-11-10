package Visitor;

import Parser.*;

// AstVisitor (visitante del AST): define una visita para cada clase de nodo.
// Permite recorrer el mismo árbol para distintas tareas sin cambiar el Parser.
public interface AstVisitor {
    // visit (visitar): procesa un nodo de tipo ProgramNode.
    void visit(ProgramNode node);

    // visit (visitar): procesa un nodo de tipo DeclarationNode.
    void visit(DeclarationNode node);

    // visit (visitar): procesa un nodo de tipo BlockNode.
    void visit(BlockNode node);

    // visit (visitar): procesa un nodo de tipo AssignmentNode.
    void visit(AssignmentNode node);

    // visit (visitar): procesa un nodo de tipo IfNode.
    void visit(IfNode node);

    // visit (visitar): procesa un nodo de tipo WhileNode.
    void visit(WhileNode node);

    // visit (visitar): procesa un nodo de tipo BinaryOperationNode.
    void visit(BinaryOperationNode node);

    // visit (visitar): procesa un nodo de tipo UnaryOperationNode.
    void visit(UnaryOperationNode node);

    // visit (visitar): procesa un nodo de tipo IdentifierNode.
    void visit(IdentifierNode node);

    // visit (visitar): procesa un nodo de tipo IntegerLiteralNode.
    void visit(IntegerLiteralNode node);

    // visit (visitar): procesa un nodo de tipo RealLiteralNode.
    void visit(RealLiteralNode node);

    // visit (visitar): procesa un nodo de tipo BooleanLiteralNode.
    void visit(BooleanLiteralNode node);

}
