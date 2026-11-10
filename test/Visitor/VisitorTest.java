package Visitor;

import Lexer.Lexer;
import Lexer.TokenType;
import Parser.*;
import Semantic.SemanticAnalyzer;
import SymbolTable.SymbolTable;

// VisitorTest (pruebas del Visitor): comprueba tipos decorados y símbolos enlazados.
public class VisitorTest {
    private static int passed = 0;

    // check (comprobar): registra una verificación o muestra dónde falló.
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("Falló una prueba del Visitor: " + message);
        }
        passed++;
    }

    // parse (analizar): convierte el código Pascal en un AST.
    private static ProgramNode parse(String source) {
        Parser parser = new Parser(new Lexer(source));
        return parser.parseProgram();
    }

    // analyze (analizar): decora el AST utilizando la tabla de símbolos.
    private static SymbolTable analyze(ProgramNode program) {
        SymbolTable table = new SymbolTable(program);
        SemanticAnalyzer analyzer = new SemanticAnalyzer(table);
        analyzer.analyze(program);
        return table;
    }

    // source (programa): construye una entrada Pascal para las pruebas.
    private static String source(String declarations, String statements) {
        return "PROGRAM Demo;\n" + declarations + "\nBEGIN\n" + statements + "\nEND.";
    }

    // checkLiterals (revisar literales): comprueba que se guarden sus tipos.
    private static void checkLiterals() {
        ProgramNode program = parse(source("VAR a: INTEGER; r: REAL; b: BOOLEAN;",
                "a := 5; r := 2.5; b := TRUE;"));
        BlockNode block = program.getBody();
        check(block.getInferredType() == null, "un bloque no debe tener tipo de expresión");
        check(block.getStatements().size() == 3, "debe haber tres asignaciones");
        analyze(program);

        AssignmentNode a = (AssignmentNode) block.getStatements().get(0);
        AssignmentNode r = (AssignmentNode) block.getStatements().get(1);
        AssignmentNode b = (AssignmentNode) block.getStatements().get(2);
        check(a.getTargetSymbol() != null, "la asignación de a debe guardar su símbolo");
        check(a.getTargetSymbol().getType() == TokenType.INTEGER, "destino INTEGER");
        check(a.getExpression().getInferredType() == TokenType.INTEGER, "literal INTEGER");
        check(r.getExpression().getInferredType() == TokenType.REAL, "literal REAL");
        check(b.getExpression().getInferredType() == TokenType.BOOLEAN, "literal BOOLEAN");
        check(program.getInferredType() == null, "el programa no debe tener tipo");
        check(a.getInferredType() == null, "la asignación no debe tener tipo de expresión");
    }

    // checkNumericExpressions (revisar operaciones): valida tipos intermedios.
    private static void checkNumericExpressions() {
        ProgramNode program = parse(source("VAR r: REAL; b: BOOLEAN;",
                "r := 2 + 3.5 * 4; r := 9 / 2; b := r >= 3;"));
        analyze(program);
        BlockNode block = program.getBody();
        AssignmentNode first = (AssignmentNode) block.getStatements().get(0);
        BinaryOperationNode sum = (BinaryOperationNode) first.getExpression();
        check(sum.getOperator() == TokenType.SUM, "la raíz debe ser SUM");
        check(sum.getInferredType() == TokenType.REAL, "suma mixta REAL");
        check(sum.getLeft().getInferredType() == TokenType.INTEGER, "izquierda INTEGER");
        BinaryOperationNode mul = (BinaryOperationNode) sum.getRight();
        check(mul.getOperator() == TokenType.MUL, "la derecha debe ser multiplicación");
        check(mul.getInferredType() == TokenType.REAL, "multiplicación mixta REAL");
        check(mul.getLeft().getInferredType() == TokenType.REAL, "literal real en multiplicación");
        check(mul.getRight().getInferredType() == TokenType.INTEGER, "literal entero en multiplicación");

        AssignmentNode second = (AssignmentNode) block.getStatements().get(1);
        BinaryOperationNode div = (BinaryOperationNode) second.getExpression();
        check(div.getOperator() == TokenType.DIV, "segunda operación DIV");
        check(div.getInferredType() == TokenType.REAL, "INTEGER / INTEGER resulta REAL");
        check(div.getLeft().getInferredType() == TokenType.INTEGER, "dividendo INTEGER");
        check(div.getRight().getInferredType() == TokenType.INTEGER, "divisor INTEGER");

        AssignmentNode third = (AssignmentNode) block.getStatements().get(2);
        BinaryOperationNode comparison = (BinaryOperationNode) third.getExpression();
        check(comparison.getInferredType() == TokenType.BOOLEAN, "comparación BOOLEAN");
        check(comparison.getLeft().getInferredType() == TokenType.REAL, "identificador real");
        check(comparison.getRight().getInferredType() == TokenType.INTEGER, "comparando entero");
    }

    // checkSymbols (revisar símbolos): comprueba los enlaces a la tabla.
    private static void checkSymbols() {
        ProgramNode program = parse(source("VAR Counter, total: INTEGER;",
                "counter := 1; TOTAL := COUNTER + 2;"));
        SymbolTable table = analyze(program);
        BlockNode block = program.getBody();
        AssignmentNode first = (AssignmentNode) block.getStatements().get(0);
        AssignmentNode second = (AssignmentNode) block.getStatements().get(1);
        BinaryOperationNode sum = (BinaryOperationNode) second.getExpression();
        IdentifierNode counter = (IdentifierNode) sum.getLeft();
        check(first.getTargetSymbol() == table.lookup("COUNTER"), "primer destino enlazado");
        check(second.getTargetSymbol() == table.lookup("total"), "segundo destino enlazado");
        check(counter.getSymbol() == table.lookup("counter"), "uso de COUNTER enlazado");
        check(counter.getSymbol() == first.getTargetSymbol(), "ambas referencias usan el mismo símbolo");
        check(counter.getInferredType() == TokenType.INTEGER, "identificador decorado INTEGER");
        check(sum.getInferredType() == TokenType.INTEGER, "suma decorada INTEGER");
        check(table.lookup("COUNTER").getName().equals("Counter"), "nombre original conservado");
    }

    // checkControlFlow (revisar control): comprueba condiciones y ramas decoradas.
    private static void checkControlFlow() {
        ProgramNode program = parse(source("VAR c: INTEGER; flag: BOOLEAN;",
                "c := 0; flag := TRUE; WHILE (c < 3) AND flag DO BEGIN "
                + "IF c = 1 THEN flag := FALSE ELSE flag := TRUE; c := c + 1; END;"));
        analyze(program);
        WhileNode loop = (WhileNode) program.getBody().getStatements().get(2);
        check(loop.getCondition().getInferredType() == TokenType.BOOLEAN, "condición de WHILE booleana");
        BinaryOperationNode and = (BinaryOperationNode) loop.getCondition();
        check(and.getOperator() == TokenType.AND, "condición AND");
        check(and.getLeft().getInferredType() == TokenType.BOOLEAN, "comparación del WHILE");
        check(and.getRight().getInferredType() == TokenType.BOOLEAN, "identificador del WHILE");
        BlockNode body = (BlockNode) loop.getBody();
        IfNode decision = (IfNode) body.getStatements().get(0);
        check(decision.getCondition().getInferredType() == TokenType.BOOLEAN, "condición del IF decorada");
        check(((AssignmentNode) decision.getThenBranch()).getTargetSymbol() != null, "destino THEN enlazado");
        check(((AssignmentNode) decision.getElseBranch()).getTargetSymbol() != null, "destino ELSE enlazado");
        check(body.getStatements().size() == 2, "dos sentencias dentro del ciclo");
    }

    // checkUnaryAndBoolean (revisar unarios): comprueba NOT y signos.
    private static void checkUnaryAndBoolean() {
        ProgramNode program = parse(source("VAR b: BOOLEAN; r: REAL;",
                "b := NOT FALSE; r := -2.5;"));
        analyze(program);
        UnaryOperationNode not = (UnaryOperationNode) ((AssignmentNode) program.getBody().getStatements().get(0)).getExpression();
        UnaryOperationNode minus = (UnaryOperationNode) ((AssignmentNode) program.getBody().getStatements().get(1)).getExpression();
        check(not.getOperator() == TokenType.NOT, "NOT representado por operador unario");
        check(not.getInferredType() == TokenType.BOOLEAN, "NOT devuelve BOOLEAN");
        check(not.getOperand().getInferredType() == TokenType.BOOLEAN, "operando de NOT BOOLEAN");
        check(minus.getOperator() == TokenType.SUB, "menos unario representado");
        check(minus.getInferredType() == TokenType.REAL, "menos real devuelve REAL");
        check(minus.getOperand().getInferredType() == TokenType.REAL, "operando de menos REAL");
    }

    // checkPrinting (revisar impresión): prueba un segundo Visitor independiente.
    private static void checkPrinting() {
        ProgramNode program = parse(source("VAR x: INTEGER; b: BOOLEAN;",
                "x := 1; b := NOT FALSE; IF b THEN BEGIN x := x + 2; END ELSE x := 0; "
                + "WHILE x < 4 DO x := x + 1;"));
        AstPrinterVisitor before = new AstPrinterVisitor();
        program.accept(before);
        check(before.getText().contains("Programa Demo"), "visita ProgramNode antes del semántico");
        check(before.getText().contains("Declaración x : INTEGER"), "visita DeclarationNode");
        check(!before.getText().contains("Operación SUM : INTEGER"), "aún no hay tipo decorado");

        analyze(program);
        AstPrinterVisitor after = new AstPrinterVisitor();
        program.accept(after);
        String text = after.getText();
        check(text.contains("Programa Demo"), "título del AST");
        check(text.contains("Declaración b : BOOLEAN"), "otra declaración");
        check(text.contains("Operación unaria NOT : BOOLEAN"), "imprime NOT decorado");
        check(text.contains("Operación SUM : INTEGER"), "imprime suma decorada");
        check(text.contains("Operación LESS_THAN : BOOLEAN"), "imprime comparación booleana");
        check(text.contains("IF") && text.contains("THEN:") && text.contains("ELSE:"), "imprime ambas ramas");
        check(text.contains("WHILE") && text.contains("Cuerpo:"), "imprime cuerpo WHILE");
        check(text.contains("Identificador x : INTEGER"), "imprime símbolo decorado");
        check(text.contains("Asignación a x : INTEGER"), "imprime destino enlazado");
    }

    // checkErrors (revisar errores): confirma que Visitor conserve el Hito E.
    private static void checkErrors() {
        checkInvalid(source("VAR x: INTEGER;", "x := TRUE;"), "no se puede asignar");
        checkInvalid(source("VAR x: INTEGER;", "x := y;"), "no está declarada");
        checkInvalid(source("VAR x,y: INTEGER;", "y := x;"), "no está inicializada");
        checkInvalid(source("VAR x: INTEGER;", "IF 5 THEN x := 1;"), "la condición de IF");
        checkInvalid(source("VAR x: BOOLEAN;", "x := TRUE + FALSE;"), "aritmético");
    }

    // checkInvalid (revisar error): debe aparecer el error semántico indicado.
    private static void checkInvalid(String source, String expected) {
        try {
            ProgramNode program = parse(source);
            analyze(program);
        } catch (RuntimeException exception) {
            check(exception.getMessage().contains(expected), "se esperaba: " + expected);
            return;
        }
        check(false, "faltó el error: " + expected);
    }

    // main (ejecutar pruebas): comprueba los nodos decorados y los dos visitantes.
    public static void main(String[] args) {
        checkLiterals();
        checkNumericExpressions();
        checkSymbols();
        checkControlFlow();
        checkUnaryAndBoolean();
        checkPrinting();
        checkErrors();
        System.out.println("Pruebas del Visitor y AST decorado superadas: " + passed);
    }
}
