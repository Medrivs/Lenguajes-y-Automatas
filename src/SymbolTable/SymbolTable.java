package SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import Lexer.TokenType;
import Parser.DeclarationNode;
import Parser.ProgramNode;

// SymbolTable (tabla de símbolos): guarda las variables del ámbito global.
public class SymbolTable {
    private final HashMap<String, Symbol> symbols;
    private final String programName;

    // SymbolTable (constructor): recibe el AST y registra todas sus declaraciones.
    public SymbolTable(ProgramNode program) {
        symbols = new HashMap<>();
        programName = normalizeName(program.getName());

        for (DeclarationNode declaration : program.getDeclarations()) {
            for (String name : declaration.getNames()) {
                insert(name, declaration.getDeclaredType(),
                        declaration.getLine(), declaration.getColumn());
            }
        }
    }

    // normalizeName (normalizar nombre): usa mayúsculas para comparar sin importar el caso.
    private String normalizeName(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    // insert (insertar): registra una variable si no está repetida ni usa el nombre del programa.
    public void insert(String name, TokenType type, int line, int column) {
        String key = normalizeName(name);

        if (key.equals(programName)) {
            throw new RuntimeException("Error semántico en la línea " + line +
                    ", columna " + column + ": la variable '" + name +
                    "' no puede tener el mismo nombre que el programa");
        }

        if (symbols.containsKey(key)) {
            throw new RuntimeException("Error semántico en la línea " + line +
                    ", columna " + column + ": la variable '" + name +
                    "' ya fue declarada");
        }

        symbols.put(key, new Symbol(name, type, line, column));
    }

    // lookup (buscar): devuelve la variable por su nombre o null si no existe.
    public Symbol lookup(String name) {
        return symbols.get(normalizeName(name));
    }

    // contains (existe): indica si una variable ya está registrada.
    public boolean contains(String name) {
        return symbols.containsKey(normalizeName(name));
    }

    // size (cantidad): devuelve cuántas variables tiene la tabla.
    public int size() {
        return symbols.size();
    }

    // getSymbols (obtener símbolos): devuelve una lista con las variables registradas.
    public List<Symbol> getSymbols() {
        return new ArrayList<>(symbols.values());
    }
}
