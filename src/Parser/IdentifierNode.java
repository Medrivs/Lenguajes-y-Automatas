package Parser;

import Visitor.AstVisitor;
import SymbolTable.Symbol;

// IdentifierNode: representa el uso de una variable.
public class IdentifierNode extends AstNode {
    private final String name;
    private Symbol symbol;

    // IdentifierNode (constructor): crea el nodo y guarda sus datos.
    public IdentifierNode(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    // getName (consultar dato): devuelve el nombre original del identificador.
    public String getName() {
        return name;
    }

    // getSymbol (obtener símbolo): devuelve la declaración a la que se refiere este nombre.
    public Symbol getSymbol() {
        return symbol;
    }

    // setSymbol (guardar símbolo): enlaza el identificador con su declaración.
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    // accept (aceptar visitante): llama al método correspondiente del visitante.
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
