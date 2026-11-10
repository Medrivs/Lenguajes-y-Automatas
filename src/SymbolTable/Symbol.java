package SymbolTable;

import Lexer.TokenType;

// Symbol (símbolo): guarda la información de una variable declarada.
public class Symbol {
    private final String name;
    private final TokenType type;
    private final int line;
    private final int column;

    // Symbol (constructor): recibe el nombre, tipo y posición de la declaración.
    public Symbol(String name, TokenType type, int line, int column) {
        this.name = name;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    // getName (obtener nombre): devuelve el nombre escrito en la declaración.
    public String getName() {
        return name;
    }

    // getType (obtener tipo): devuelve INTEGER, REAL o BOOLEAN.
    public TokenType getType() {
        return type;
    }

    // getLine (obtener línea): indica la línea de la declaración.
    public int getLine() {
        return line;
    }

    // getColumn (obtener columna): indica la columna inicial de la declaración.
    public int getColumn() {
        return column;
    }

    // toString (convertir a texto): muestra los datos del símbolo para las pruebas.
    @Override
    public String toString() {
        return name + " : " + type + " (línea " + line + ", columna " + column + ")";
    }
}
