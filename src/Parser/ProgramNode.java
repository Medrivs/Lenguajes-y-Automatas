package Parser;

import java.util.List;

// ProgramNode: representa el programa completo.
public class ProgramNode extends AstNode {
    private final String name;
    private final List<DeclarationNode> declarations;
    private final BlockNode body;

    // ProgramNode (constructor): crea el nodo y guarda sus datos.
    public ProgramNode(String name, List<DeclarationNode> declarations, BlockNode body, int line, int column) {
        super(line, column);
        this.name = name;
        this.declarations = declarations;
        this.body = body;
    }

    // getName (consultar dato): devuelve el nombre del programa.
    public String getName() {
        return name;
    }

    // getDeclarations (consultar dato): devuelve las declaraciones de variables.
    public List<DeclarationNode> getDeclarations() {
        return declarations;
    }

    // getBody (consultar dato): devuelve el bloque principal.
    public BlockNode getBody() {
        return body;
    }

}
