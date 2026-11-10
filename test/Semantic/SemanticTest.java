package Semantic;

import Lexer.Lexer;
import Parser.Parser;
import Parser.ProgramNode;
import SymbolTable.SymbolTable;

// SemanticTest (pruebas semánticas): revisa programas válidos e inválidos.
public class SemanticTest {
    private static int passed = 0;

    // compile (compilar): genera el AST, la tabla y ejecuta el análisis semántico.
    private static void compile(String source) {
        Parser parser = new Parser(new Lexer(source));
        ProgramNode program = parser.parseProgram();
        SymbolTable table = new SymbolTable(program);
        SemanticAnalyzer analyzer = new SemanticAnalyzer(table);
        analyzer.analyze(program);
    }

    // valid (válido): comprueba que un programa no produzca errores.
    private static void valid(String name, String source) {
        try {
            compile(source);
            passed++;
        } catch (RuntimeException ex) {
            throw new RuntimeException("Falló el caso válido '" + name + "': " + ex.getMessage());
        }
    }

    // invalid (inválido): espera un error con una palabra o frase determinada.
    private static void invalid(String name, String source, String expected) {
        try {
            compile(source);
        } catch (RuntimeException ex) {
            if (!ex.getMessage().contains(expected)) {
                throw new RuntimeException("Falló el caso inválido '" + name
                        + "': se obtuvo '" + ex.getMessage() + "', se esperaba '" + expected + "'");
            }
            passed++;
            return;
        }
        throw new RuntimeException("Falló el caso inválido '" + name + "': no se encontró ningún error");
    }

    // program (programa): arma una fuente Pascal con declaraciones y sentencias.
    private static String program(String declarations, String statements) {
        return "PROGRAM Demo;\n" + declarations + "\nBEGIN\n" + statements + "\nEND.";
    }

    // manyDigits (crear dígitos): forma un número largo para probar los límites.
    private static String manyDigits(char digit, int amount) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            text.append(digit);
        }
        return text.toString();
    }

    // main (pruebas): ejecuta las verificaciones desde NetBeans.
    public static void main(String[] args) {
        valid("programa vacío", program("", ""));
        valid("bloque vacío", program("", "BEGIN END;"));
        valid("entero", program("VAR x: INTEGER;", "x := 10;"));
        valid("real", program("VAR x: REAL;", "x := 3.14;"));
        valid("booleano", program("VAR x: BOOLEAN;", "x := TRUE;"));
        valid("entero a real", program("VAR x: REAL;", "x := 5;"));
        valid("aritmética mixta", program("VAR x: REAL;", "x := 5 + 1.5 * 2;"));
        valid("operadores aritméticos", program("VAR x: INTEGER;", "x := +5 - 2 * 3 + 10;"));
        valid("división entera con resultado real", program("VAR x: REAL;", "x := 9 / 2;"));
        valid("signo menos unario", program("VAR x: INTEGER;", "x := -5;"));
        valid("real con signo unario", program("VAR x: REAL;", "x := -2.5;"));
        valid("entero máximo", program("VAR x: INTEGER;", "x := 2147483647;"));
        valid("ceros iniciales", program("VAR x: INTEGER;", "x := 0000000000000000000000001;"));
        valid("muchos ceros", program("VAR x: INTEGER;", "x := " + manyDigits('0', 250) + ";"));
        valid("operador AND", program("VAR x: BOOLEAN;", "x := TRUE AND FALSE;"));
        valid("operador OR", program("VAR x: BOOLEAN;", "x := TRUE OR FALSE;"));
        valid("operador NOT", program("VAR x: BOOLEAN;", "x := NOT FALSE;"));
        valid("igualdad entre booleanos", program("VAR x: BOOLEAN;", "x := TRUE = FALSE;"));
        valid("desigualdad entre booleanos", program("VAR x: BOOLEAN;", "x := TRUE <> FALSE;"));
        valid("comparaciones numéricas", program("VAR x: BOOLEAN;", "x := 2.0 >= 1;"));
        valid("otras comparaciones", program("VAR a,b,c,d: BOOLEAN;", "a := 1 < 2; b := 2 <= 3; c := 3 > 2; d := 4 <> 5;"));
        valid("lectura de variable", program("VAR a,b: INTEGER;", "a := 1; b := a + 2;"));
        valid("sin distinción de mayúsculas", program("VAR Value: INTEGER;", "vAlUe := 1; VALUE := value + 1;"));
        valid("condicional IF con ELSE", program("VAR x: INTEGER; b: BOOLEAN;", "b := TRUE; IF b THEN x := 1 ELSE x := 2; x := x + 1;"));
        valid("ramas anidadas", program("VAR x: INTEGER; b: BOOLEAN;", "b := TRUE; IF b THEN BEGIN x := 1; END ELSE BEGIN x := 2; END; x := x + 1;"));
        valid("IF con variable ya inicializada", program("VAR x: INTEGER;", "x := 1; IF TRUE THEN x := 2; x := x + 1;"));
        valid("WHILE con variable ya inicializada", program("VAR x: INTEGER;", "x := 1; WHILE x < 3 DO x := x + 1;"));
        valid("inicialización dentro del WHILE", program("VAR x: INTEGER;", "WHILE TRUE DO BEGIN x := 1; x := x + 1; END;"));
        valid("igualdad numérica que produce BOOLEAN", program("VAR x: BOOLEAN;", "x := (1 = 1.0) AND NOT FALSE;"));
        valid("dos bloques vacíos anidados", program("", "BEGIN BEGIN END; END;"));
        valid("IF sin ELSE ni lectura posterior", program("VAR x: INTEGER;", "IF TRUE THEN x := 1;"));
        valid("WHILE sin lectura posterior", program("VAR x: INTEGER;", "WHILE FALSE DO x := 1;"));
        valid("comparación con paréntesis", program("VAR b: BOOLEAN;", "b := (1 < 2) AND (2 < 3);"));
        valid("igualdad de enteros y reales", program("VAR b: BOOLEAN;", "b := 1 = 1.0;"));
        valid("dos lecturas consecutivas", program("VAR x,y: INTEGER;", "x := 0; y := x; x := y;"));

        invalid("asignación sin declaración", program("", "x := 1;"), "no está declarada");
        invalid("lectura sin declaración", program("VAR x: INTEGER;", "x := y;"), "no está declarada");
        invalid("lectura sin inicialización", program("VAR x,y: INTEGER;", "y := x;"), "no está inicializada");
        invalid("lectura de la misma variable", program("VAR x: INTEGER;", "x := x + 1;"), "no está inicializada");
        invalid("real a entero", program("VAR x: INTEGER;", "x := 3.5;"), "no se puede asignar un valor de tipo REAL");
        invalid("división a entero", program("VAR x: INTEGER;", "x := 2 / 1;"), "no se puede asignar un valor de tipo REAL");
        invalid("booleano a entero", program("VAR x: INTEGER;", "x := TRUE;"), "no se puede asignar un valor de tipo BOOLEAN");
        invalid("entero a booleano", program("VAR x: BOOLEAN;", "x := 1;"), "no se puede asignar un valor de tipo INTEGER");
        invalid("aritmética con booleanos", program("VAR x: BOOLEAN;", "x := TRUE + FALSE;"), "aritmético");
        invalid("multiplicación con booleanos", program("VAR x: BOOLEAN;", "x := TRUE * TRUE;"), "aritmético");
        invalid("división con booleanos", program("VAR x: REAL;", "x := TRUE / 2;"), "división");
        invalid("AND con números", program("VAR x: BOOLEAN;", "x := 1 AND 2;"), "AND y OR");
        invalid("OR con números", program("VAR x: BOOLEAN;", "x := 1 OR 2;"), "AND y OR");
        invalid("NOT con entero", program("VAR x: BOOLEAN;", "x := NOT 1;"), "NOT requiere");
        invalid("booleano con signo negativo", program("VAR x: BOOLEAN;", "x := -TRUE;"), "unario");
        invalid("booleano con signo positivo", program("VAR x: BOOLEAN;", "x := +TRUE;"), "unario");
        invalid("comparación de booleanos", program("VAR x: BOOLEAN;", "x := TRUE < FALSE;"), "comparación");
        invalid("igualdad mixta con booleano", program("VAR x: BOOLEAN;", "x := TRUE = 1;"), "= y <>");
        invalid("IF con condición entera", program("VAR x: INTEGER;", "IF 1 THEN x := 1;"), "la condición de IF");
        invalid("IF con condición real", program("VAR x: INTEGER;", "IF 1.5 THEN x := 1;"), "la condición de IF");
        invalid("WHILE con condición entera", program("VAR x: INTEGER;", "WHILE 1 DO x := 1;"), "la condición de WHILE");
        invalid("IF sin ELSE", program("VAR x,y: INTEGER;", "IF TRUE THEN x := 1; y := x;"), "no está inicializada");
        invalid("inicialización solo en THEN", program("VAR x,y: INTEGER;", "IF TRUE THEN x := 1 ELSE BEGIN END; y := x;"), "no está inicializada");
        invalid("inicialización solo en ELSE", program("VAR x,y: INTEGER;", "IF TRUE THEN BEGIN END ELSE x := 1; y := x;"), "no está inicializada");
        invalid("inicialización solo en el cuerpo de WHILE", program("VAR x,y: INTEGER;", "WHILE TRUE DO x := 1; y := x;"), "no está inicializada");
        invalid("lectura sin inicializar en condición de WHILE", program("VAR x: INTEGER;", "WHILE x < 1 DO x := 1;"), "no está inicializada");
        invalid("lectura sin inicializar en condición de IF", program("VAR x: INTEGER;", "IF x > 1 THEN x := 1;"), "no está inicializada");
        invalid("cortocircuito AND", program("VAR b: BOOLEAN;", "IF FALSE AND b THEN b := TRUE;"), "no está inicializada");
        invalid("cortocircuito OR", program("VAR b: BOOLEAN;", "IF TRUE OR b THEN b := TRUE;"), "no está inicializada");
        invalid("entero fuera de rango", program("VAR x: INTEGER;", "x := 2147483648;"), "fuera de rango");
        invalid("entero muy grande fuera de rango", program("VAR x: INTEGER;", "x := 99999999999999999999999999;"), "fuera de rango");
        invalid("real fuera de rango", program("VAR x: REAL;", "x := " + manyDigits('9', 500) + ".0;"), "fuera de rango");
        invalid("símbolos duplicados", program("VAR x: INTEGER; X: REAL;", ""), "ya fue declarada");
        invalid("nombre del programa reservado", program("VAR demo: INTEGER;", ""), "mismo nombre que el programa");
        invalid("lectura sin inicializar en rama izquierda", program("VAR b: BOOLEAN;", "IF TRUE THEN b := NOT b ELSE b := TRUE;"), "no está inicializada");

        System.out.println("Pruebas semánticas superadas: " + passed);
    }
}
