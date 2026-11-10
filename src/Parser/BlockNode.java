package Parser;

import Visitor.AstVisitor;

import java.util.ArrayList;
import java.util.List;

// BlockNode: guarda las sentencias de un BEGIN y END.
public class BlockNode extends AstNode {
    private final List<AstNode> statements;

    // BlockNode (constructor): crea el nodo y guarda sus datos.
    public BlockNode(int line, int column) {
        super(line, column);
        this.statements = new ArrayList<>();
    }

    // addStatement (agregar sentencia): añade una sentencia al final del bloque.
    public void addStatement(AstNode statement) {
        statements.add(statement);
    }

    // getStatements (consultar dato): devuelve las sentencias del bloque.
    public List<AstNode> getStatements() {
        return statements;
    }

    // accept (aceptar visitante): llama al método correspondiente del visitante.
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
