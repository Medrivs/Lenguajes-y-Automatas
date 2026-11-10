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
            throw new RuntimeException("Prueba fallida: " + description);
        }
        testCount++;
    }

    // checkInvalid (comprobar error): espera un error semántico al registrar declaraciones.
    private static void checkInvalid(String code, String expectedMessage) {
        boolean foundError = false;
        try {
            new SymbolTable(parse(code));
        } catch (RuntimeException error) {
            check(error.getMessage().contains("Error semántico en la línea "),
                    "El error debe incluir la línea de declaración: " + error.getMessage());
            check(error.getMessage().contains("columna "),
                    "El error debe incluir la columna de declaración: " + error.getMessage());
            check(error.getMessage().contains(expectedMessage),
                    "El error debe describir el problema: " + error.getMessage());
            foundError = true;
        }
        check(foundError, "El programa debería tener declaraciones inválidas: " + code);
    }

    // main (iniciar pruebas): ejecuta los casos del Hito D.
    public static void main(String[] args) {
        SymbolTable empty = new SymbolTable(parse("PROGRAM Empty; BEGIN END."));
        check(empty.size() == 0, "Un programa sin VAR no tiene símbolos");
        check(empty.lookup("x") == null, "Una variable inexistente devuelve null");
        check(!empty.contains("x"), "La variable inexistente no está registrada");
        check(!empty.contains("Empty"), "El nombre del programa no es una variable");

        SymbolTable table = new SymbolTable(parse(
                "PROGRAM Demo;\n" +
                "VAR\n" +
                "    Counter, total_1: INTEGER;\n" +
                "    Average: REAL;\n" +
                "    ready: BOOLEAN;\n" +
                "BEGIN\n" +
                "    BEGIN END;\n" +
                "END."));
        check(table.size() == 4, "Todas las variables declaradas están registradas");
        check(table.getSymbols().size() == 4, "La lista contiene todos los símbolos registrados");
        check(table.contains("counter"), "La búsqueda ignora las minúsculas");
        check(table.contains("COUNTER"), "La búsqueda ignora las mayúsculas");
        check(table.lookup("cOuNtEr") == table.lookup("COUNTER"), "El símbolo es el mismo sin importar mayúsculas");
        check(table.lookup("Counter").getName().equals("Counter"), "Se conserva la escritura original");
        check(table.lookup("Counter").getType() == TokenType.INTEGER, "Tipo INTEGER");
        check(table.lookup("total_1").getType() == TokenType.INTEGER, "Identificadores múltiples");
        check(table.lookup("Average").getType() == TokenType.REAL, "Tipo REAL");
        check(table.lookup("ready").getType() == TokenType.BOOLEAN, "Tipo BOOLEAN");
        check(table.lookup("Counter").getLine() == 3, "Línea de declaración");
        check(table.lookup("Counter").getColumn() == 5, "Columna de declaración");
        check(table.lookup("Average").getLine() == 4, "Línea de otra declaración");
        check(table.lookup("notDeclared") == null, "Búsqueda de nombre inexistente");
        check(!table.contains("Demo"), "El nombre del programa sigue reservado");
        check(table.lookup("counter").toString().contains("INTEGER"), "Representación legible del símbolo");

        checkInvalid("PROGRAM P; VAR x: INTEGER; X: REAL; BEGIN END.", "ya fue declarada");
        checkInvalid("PROGRAM P; VAR name, NAME: INTEGER; BEGIN END.", "ya fue declarada");
        checkInvalid("PROGRAM Demo; VAR demo: BOOLEAN; BEGIN END.", "mismo nombre que el programa");
        checkInvalid("PROGRAM P; VAR P: INTEGER; BEGIN END.", "mismo nombre que el programa");

        // El parser acepta referencias sin declarar: se revisarán en el Hito E.
        SymbolTable undeclared = new SymbolTable(parse("PROGRAM P; BEGIN x := 1; END."));
        check(undeclared.size() == 0, "Usar un nombre no implica declararlo");
        check(undeclared.lookup("x") == null, "Un uso sin declaración no figura en la tabla");

        System.out.println("Correcto: " + testCount + " comprobaciones de tabla de símbolos superadas.");
    }
}
