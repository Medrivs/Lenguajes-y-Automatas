package Parser;

import Lexer.Lexer;
import Lexer.TokenType;

// ParserTest (pruebas del parser): comprueba la gramática y el AST sin usar JUnit.
public class ParserTest {
    private static int testCount = 0;

    // parse (analizar): convierte un texto Pascal en su árbol de programa.
    private static ProgramNode parse(String source) {
        Parser parser = new Parser(new Lexer(source));
        return parser.parseProgram();
    }

    // check (comprobar): detiene la prueba si una condición no es verdadera.
    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new RuntimeException("Prueba fallida: " + description);
        }
        testCount++;
    }

    // checkValid (programa válido): verifica que el parser acepte el código.
    private static void checkValid(String source) {
        ProgramNode program = parse(source);
        check(program != null, "El programa debería ser válido: " + source);
    }

    // checkInvalid (programa inválido): verifica que se informe un error sintáctico.
    private static void checkInvalid(String source) {
        boolean foundError = false;
        try {
            parse(source);
        } catch (RuntimeException error) {
            check(error.getMessage().contains("Error sintáctico en la línea "),
                    "El error sintáctico debe incluir su ubicación: " + error.getMessage());
            foundError = true;
        }
        check(foundError, "El programa debería ser rechazado: " + source);
    }

    // checkPosition (comprobar ubicación): revisa línea y columna de un nodo.
    private static void checkPosition(AstNode node, int line, int column) {
        check(node.getLine() == line && node.getColumn() == column,
                "Se esperaba " + line + ":" + column + " pero se encontró " +
                node.getLine() + ":" + node.getColumn());
    }

    // main (inicio de las pruebas): ejecuta casos válidos, inválidos y de AST.
    public static void main(String[] args) {
        // Los programas mínimos no necesitan declaraciones ni sentencias.
        checkValid("PROGRAM Empty; BEGIN END.");
        checkValid("program empty; begin end.  \n");
        checkValid("PROGRAM P; BEGIN BEGIN END END.");
        checkValid("PROGRAM P; BEGIN x := 1 END.");
        checkValid("PROGRAM P; BEGIN x := 1; END.");
        checkValid("PROGRAM P; BEGIN x := 1; y := 2 END.");
        checkValid("PROGRAM P; BEGIN BEGIN x := 1; END; END.");
        checkValid("PROGRAM P; VAR x: INTEGER; BEGIN END.");
        checkValid("PROGRAM P; VAR x, y: INTEGER; z: REAL; b: BOOLEAN; BEGIN END.");
        checkValid("PROGRAM P; VAR x: INTEGER; BEGIN x := 1; END.");
        checkValid("PROGRAM P; BEGIN x := 999999999999999999999999999; END.");
        checkValid("PROGRAM P; BEGIN x := 001.50 END.");
        checkValid("PROGRAM P; BEGIN x := TRUE; y := FALSE END.");
        checkValid("PROGRAM P; BEGIN x := +2; y := -4 END.");
        checkValid("PROGRAM P; BEGIN x := NOT TRUE; END.");
        checkValid("PROGRAM P; BEGIN x := NOT NOT FALSE END.");
        checkValid("PROGRAM P; BEGIN x := (1 + 2) * 3 END.");
        checkValid("PROGRAM P; BEGIN x := 5 AND 2; END."); // Los tipos se revisan después.
        checkValid("PROGRAM P; BEGIN x := 3 / 2; END.");
        checkValid("PROGRAM P; BEGIN x := (a < b) AND (c < d); END.");
        checkValid("PROGRAM P; BEGIN IF x > 0 THEN x := 1 END.");
        checkValid("PROGRAM P; BEGIN IF x > 0 THEN x := 1 ELSE x := 2 END.");
        checkValid("PROGRAM P; BEGIN IF x > 0 THEN BEGIN END ELSE BEGIN END END.");
        checkValid("PROGRAM P; BEGIN WHILE x < 3 DO x := x + 1 END.");
        checkValid("PROGRAM P; BEGIN WHILE TRUE DO BEGIN END; END.");
        checkValid("PROGRAM P; BEGIN IF a THEN IF b THEN x := 1 ELSE x := 2 END.");
        checkValid("PROGRAM P; BEGIN IF a THEN IF b THEN x := 1 ELSE x := 2 ELSE x := 3 END.");
        checkValid("PROGRAM P; BEGIN IF a THEN WHILE b DO BEGIN x := 1 END ELSE x := 2 END.");
        checkValid("PROGRAM P; BEGIN IF a THEN BEGIN x := 1; END ELSE BEGIN x := 2; END END.");
        checkValid("PROGRAM P; BEGIN x := a + b - c OR d; END.");
        checkValid("PROGRAM P; BEGIN x := a * b / c AND d; END.");

        // Programa completo tomado del contrato oficial Pascal Subset v1.0.
        checkValid("PROGRAM Demo;\n" +
                "VAR x, y: INTEGER; promedio: REAL; permitido: BOOLEAN;\n" +
                "BEGIN\n" +
                "    x := 0; y := 3;\n" +
                "    promedio := x + y / 2;\n" +
                "    permitido := (promedio >= 1.0) AND (y <> 0);\n" +
                "    IF permitido THEN BEGIN x := x + 1; END\n" +
                "    ELSE BEGIN x := x - 1; END;\n" +
                "    WHILE x < 5 DO BEGIN x := x + 1; END;\n" +
                "END.");

        TokenType[] relations = {TokenType.EQUAL, TokenType.NOT_EQUAL,
                TokenType.LESS_THAN, TokenType.LESS_EQUAL,
                TokenType.GREATER_THAN, TokenType.GREATER_EQUAL};
        String[] relationSigns = {"=", "<>", "<", "<=", ">", ">="};
        for (int i = 0; i < relations.length; i++) {
            ProgramNode program = parse("PROGRAM P; BEGIN result := x " + relationSigns[i] + " 2 END.");
            AssignmentNode assignment = (AssignmentNode) program.getBody().getStatements().get(0);
            BinaryOperationNode expression = (BinaryOperationNode) assignment.getExpression();
            check(expression.getOperator() == relations[i], "Relational operator: " + relationSigns[i]);
        }

        // Revisamos que las declaraciones y el nombre no desaparezcan del AST.
        ProgramNode declarations = parse("PROGRAM Demo; VAR x, y: INTEGER; rate: REAL; ready: BOOLEAN; BEGIN x := 1; END.");
        check(declarations.getName().equals("Demo"), "Nombre del programa");
        check(declarations.getDeclarations().size() == 3, "Cantidad de declaraciones");
        check(declarations.getDeclarations().get(0).getNames().size() == 2, "Dos nombres en la declaración");
        check(declarations.getDeclarations().get(0).getNames().get(1).equals("y"), "Nombre de la segunda variable");
        check(declarations.getDeclarations().get(1).getDeclaredType() == TokenType.REAL, "Declaración de tipo REAL");
        check(declarations.getDeclarations().get(2).getDeclaredType() == TokenType.BOOLEAN, "Declaración de tipo BOOLEAN");
        check(declarations.getBody().getStatements().size() == 1, "Una sentencia en el cuerpo");

        // Multiplicación tiene prioridad sobre suma.
        ProgramNode sum = parse("PROGRAM P; BEGIN x := a + b * c END.");
        AssignmentNode sumAssignment = (AssignmentNode) sum.getBody().getStatements().get(0);
        BinaryOperationNode addition = (BinaryOperationNode) sumAssignment.getExpression();
        check(addition.getOperator() == TokenType.SUM, "Suma en la raíz");
        check(addition.getLeft() instanceof IdentifierNode, "Operando izquierdo de la suma");
        BinaryOperationNode multiplication = (BinaryOperationNode) addition.getRight();
        check(multiplication.getOperator() == TokenType.MUL, "Multiplicación en el operando derecho");
        check(((IdentifierNode) multiplication.getRight()).getName().equals("c"), "Nombre del identificador en el AST");

        // Las restas se agrupan de izquierda a derecha.
        ProgramNode subs = parse("PROGRAM P; BEGIN x := a - b - c END.");
        BinaryOperationNode outerSub = (BinaryOperationNode)
                ((AssignmentNode) subs.getBody().getStatements().get(0)).getExpression();
        check(outerSub.getOperator() == TokenType.SUB, "Resta exterior");
        check(outerSub.getLeft() instanceof BinaryOperationNode, "Asociatividad por la izquierda");
        check(((BinaryOperationNode) outerSub.getLeft()).getOperator() == TokenType.SUB, "Resta interior");

        // El signo unario se aplica al término completo: -(a * b) + c.
        ProgramNode unary = parse("PROGRAM P; BEGIN x := -a * b + c END.");
        BinaryOperationNode unaryRoot = (BinaryOperationNode)
                ((AssignmentNode) unary.getBody().getStatements().get(0)).getExpression();
        check(unaryRoot.getOperator() == TokenType.SUM, "Raíz del ejemplo unario");
        UnaryOperationNode minus = (UnaryOperationNode) unaryRoot.getLeft();
        check(minus.getOperator() == TokenType.SUB, "Operador unario negativo");
        check(minus.getOperand() instanceof BinaryOperationNode, "El signo unario abarca todo el término");
        check(((BinaryOperationNode) minus.getOperand()).getOperator() == TokenType.MUL,
                "El signo unario abarca la multiplicación");

        // NOT pertenece a factor; OR pertenece a simple_expression.
        ProgramNode booleanProgram = parse("PROGRAM P; BEGIN b := NOT FALSE AND TRUE OR FALSE END.");
        BinaryOperationNode boolOr = (BinaryOperationNode)
                ((AssignmentNode) booleanProgram.getBody().getStatements().get(0)).getExpression();
        check(boolOr.getOperator() == TokenType.OR, "OR en la raíz");
        BinaryOperationNode boolAnd = (BinaryOperationNode) boolOr.getLeft();
        check(boolAnd.getOperator() == TokenType.AND, "AND tiene prioridad sobre OR");
        UnaryOperationNode boolNot = (UnaryOperationNode) boolAnd.getLeft();
        check(boolNot.getOperator() == TokenType.NOT, "NOT tiene prioridad sobre AND");
        check(!((BooleanLiteralNode) boolNot.getOperand()).getValue(), "Valor del literal FALSE");
        check(((BooleanLiteralNode) boolAnd.getRight()).getValue(), "Valor del literal TRUE");

        // Los textos originales de enteros y reales permanecen en el AST.
        ProgramNode literals = parse("PROGRAM P; BEGIN i := 0001; r := 001.50 END.");
        IntegerLiteralNode integer = (IntegerLiteralNode)
                ((AssignmentNode) literals.getBody().getStatements().get(0)).getExpression();
        RealLiteralNode real = (RealLiteralNode)
                ((AssignmentNode) literals.getBody().getStatements().get(1)).getExpression();
        check(integer.getLexeme().equals("0001"), "Lexema original del entero");
        check(real.getLexeme().equals("001.50"), "Lexema original del real");

        // ELSE pertenece al IF más cercano sin ELSE.
        ProgramNode dangling = parse("PROGRAM P; BEGIN IF a THEN IF b THEN x := 1 ELSE x := 2 END.");
        IfNode outerIf = (IfNode) dangling.getBody().getStatements().get(0);
        check(outerIf.getElseBranch() == null, "El IF exterior no tiene ELSE");
        IfNode innerIf = (IfNode) outerIf.getThenBranch();
        check(innerIf.getElseBranch() instanceof AssignmentNode, "El ELSE pertenece al IF interior");

        // Los bloques pueden ser vacíos; el WHILE guarda su cuerpo sin ejecutarlo.
        ProgramNode nested = parse("PROGRAM P; BEGIN WHILE TRUE DO BEGIN END END.");
        WhileNode whileNode = (WhileNode) nested.getBody().getStatements().get(0);
        check(whileNode.getBody() instanceof BlockNode, "WHILE contiene un bloque");
        check(((BlockNode) whileNode.getBody()).getStatements().isEmpty(), "Bloque anidado vacío");

        // Las ubicaciones se conservan en el AST.
        ProgramNode positioned = parse("PROGRAM P;\r\nVAR\r\n x: REAL;\r\nBEGIN\r\n x := 1.5;\r\nEND.");
        checkPosition(positioned, 1, 1);
        checkPosition(positioned.getDeclarations().get(0), 3, 2);
        checkPosition(positioned.getBody(), 4, 1);
        AssignmentNode positionedAssign = (AssignmentNode) positioned.getBody().getStatements().get(0);
        checkPosition(positionedAssign, 5, 2);
        checkPosition(positionedAssign.getExpression(), 5, 7);

        // Los siguientes casos deben fallar por sintaxis, no por reglas de tipos.
        checkInvalid("");
        checkInvalid("BEGIN END.");
        checkInvalid("PROGRAM; BEGIN END.");
        checkInvalid("PROGRAM P BEGIN END.");
        checkInvalid("PROGRAM P; BEGIN END");
        checkInvalid("PROGRAM P; BEGIN END. x");
        checkInvalid("PROGRAM P; BEGIN END.. ");
        checkInvalid("PROGRAM P; VAR BEGIN END.");
        checkInvalid("PROGRAM P; VAR x: INTEGER BEGIN END.");
        checkInvalid("PROGRAM P; VAR x, : INTEGER; BEGIN END.");
        checkInvalid("PROGRAM P; VAR x: FLOAT; BEGIN END.");
        checkInvalid("PROGRAM P; VAR x:; BEGIN END.");
        checkInvalid("PROGRAM P; VAR x: INTEGER; VAR y: INTEGER; BEGIN END.");
        checkInvalid("PROGRAM P; VAR x: INTEGER; x: INTEGER; BEGIN VAR y: REAL; END.");
        checkInvalid("PROGRAM P; BEGIN ; END.");
        checkInvalid("PROGRAM P; BEGIN x := 1;; END.");
        checkInvalid("PROGRAM P; BEGIN x := 1 y := 2 END.");
        checkInvalid("PROGRAM P; BEGIN x := 1; ELSE x := 2 END.");
        checkInvalid("PROGRAM P; BEGIN IF TRUE THEN x := 1; ELSE x := 2 END.");
        checkInvalid("PROGRAM P; BEGIN IF TRUE x := 1 END.");
        checkInvalid("PROGRAM P; BEGIN IF TRUE THEN END.");
        checkInvalid("PROGRAM P; BEGIN WHILE TRUE x := 1 END.");
        checkInvalid("PROGRAM P; BEGIN WHILE TRUE DO END.");
        checkInvalid("PROGRAM P; BEGIN x := ; END.");
        checkInvalid("PROGRAM P; BEGIN x := 1 < 2 < 3 END.");
        checkInvalid("PROGRAM P; BEGIN x := a < b AND c < d END.");
        checkInvalid("PROGRAM P; BEGIN x := NOT END.");
        checkInvalid("PROGRAM P; BEGIN x := --2 END.");
        checkInvalid("PROGRAM P; BEGIN x := (1 + 2 END.");
        checkInvalid("PROGRAM P; BEGIN x := 1.; END.");
        checkInvalid("PROGRAM P; BEGIN IF TRUE THEN x := 1 ELSE END.");
        checkInvalid("PROGRAM P; BEGIN END;.");
        checkInvalid("PROGRAM P; BEGIN BEGIN VAR x: INTEGER; END END.");

        // Comprueba que el mensaje señale el símbolo inesperado.
        try {
            parse("PROGRAM P;\nBEGIN\n    x := ;\nEND.");
            throw new RuntimeException("Se esperaba un error sintáctico");
        } catch (RuntimeException error) {
            check(error.getMessage().contains("línea 3, columna 10"), "Ubicación exacta del error");
            check(error.getMessage().contains("SEMICOLON"), "Token inesperado indicado en el error");
        }

        System.out.println("Correcto: " + testCount + " comprobaciones del parser superadas.");
    }
}
