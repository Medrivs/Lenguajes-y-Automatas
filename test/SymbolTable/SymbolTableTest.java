package SymbolTable;

import Lexer.Lexer;
import Lexer.TokenType;
import Parser.Parser;
import Parser.ProgramNode;

// SymbolTableTest (pruebas): verifica la tabla de símbolos sin utilizar JUnit.
public class SymbolTableTest {
    private static int testCount = 0;

    // parse (analizar): construye el AST de un programa Pascal.
    private static ProgramNode parse(String code) {
        Parser parser = new Parser(new Lexer(code));
        return parser.parseProgram();
    }

    // check (comprobar): detiene la prueba si el resultado no es el esperado.
    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new RuntimeException("Test failed: " + description);
        }
        testCount++;
    }

    // checkInvalid (comprobar error): espera un error semántico al registrar declaraciones.
    private static void checkInvalid(String code, String expectedMessage) {
        boolean foundError = false;
        try {
            new SymbolTable(parse(code));
        } catch (RuntimeException error) {
            check(error.getMessage().contains("Semantic error at line "),
                    "Error must include the declaration line: " + error.getMessage());
            check(error.getMessage().contains("column "),
                    "Error must include the declaration column: " + error.getMessage());
            check(error.getMessage().contains(expectedMessage),
                    "Error must describe the problem: " + error.getMessage());
            foundError = true;
        }
        check(foundError, "Program should contain invalid declarations: " + code);
    }

    // main (iniciar pruebas): ejecuta los casos del Hito D.
    public static void main(String[] args) {
        SymbolTable empty = new SymbolTable(parse("PROGRAM Empty; BEGIN END."));
        check(empty.size() == 0, "Program without VAR has no symbols");
        check(empty.lookup("x") == null, "Unknown variable returns null");
        check(!empty.contains("x"), "Unknown variable is not registered");
        check(!empty.contains("Empty"), "Program name is not a variable");

        SymbolTable table = new SymbolTable(parse(
                "PROGRAM Demo;\n" +
                "VAR\n" +
                "    Counter, total_1: INTEGER;\n" +
                "    Average: REAL;\n" +
                "    ready: BOOLEAN;\n" +
                "BEGIN\n" +
                "    BEGIN END;\n" +
                "END."));
        check(table.size() == 4, "All declared variables registered");
        check(table.getSymbols().size() == 4, "List returns all registered symbols");
        check(table.contains("counter"), "Lookup ignores lowercase");
        check(table.contains("COUNTER"), "Lookup ignores uppercase");
        check(table.lookup("cOuNtEr") == table.lookup("COUNTER"), "Same symbol for any case");
        check(table.lookup("Counter").getName().equals("Counter"), "Original spelling preserved");
        check(table.lookup("Counter").getType() == TokenType.INTEGER, "Integer type");
        check(table.lookup("total_1").getType() == TokenType.INTEGER, "Multiple identifiers");
        check(table.lookup("Average").getType() == TokenType.REAL, "Real type");
        check(table.lookup("ready").getType() == TokenType.BOOLEAN, "Boolean type");
        check(table.lookup("Counter").getLine() == 3, "Declaration line");
        check(table.lookup("Counter").getColumn() == 5, "Declaration column");
        check(table.lookup("Average").getLine() == 4, "Another declaration line");
        check(table.lookup("notDeclared") == null, "Lookup missing name");
        check(!table.contains("Demo"), "Program name remains reserved");
        check(table.lookup("counter").toString().contains("INTEGER"), "Readable symbol output");

        checkInvalid("PROGRAM P; VAR x: INTEGER; X: REAL; BEGIN END.", "already declared");
        checkInvalid("PROGRAM P; VAR name, NAME: INTEGER; BEGIN END.", "already declared");
        checkInvalid("PROGRAM Demo; VAR demo: BOOLEAN; BEGIN END.", "same name as the program");
        checkInvalid("PROGRAM P; VAR P: INTEGER; BEGIN END.", "same name as the program");

        // El parser acepta referencias sin declarar: se revisarán en el Hito E.
        SymbolTable undeclared = new SymbolTable(parse("PROGRAM P; BEGIN x := 1; END."));
        check(undeclared.size() == 0, "Using a name does not declare it");
        check(undeclared.lookup("x") == null, "Undeclared use is not in the table");

        System.out.println("SymbolTableTest: " + testCount + " checks passed.");
    }
}
