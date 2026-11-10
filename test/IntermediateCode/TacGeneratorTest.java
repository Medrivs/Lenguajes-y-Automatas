package IntermediateCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import Lexer.Lexer;
import Lexer.TokenType;
import Parser.Parser;
import Parser.ProgramNode;
import Semantic.SemanticAnalyzer;
import SymbolTable.SymbolTable;

// TacGeneratorTest (pruebas TAC): comprueba estructura, tipos y flujo de ejecución.
// Las pruebas no necesitan JUnit y se pueden ejecutar con Run File en NetBeans.
public class TacGeneratorTest {
    private static int passed = 0;

    // check (comprobar): exige una condición verdadera y contabiliza el caso.
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("Falló una prueba TAC: " + message);
        }
        passed++;
    }

    // ensure (asegurar): valida la simulación sin aumentar el contador de casos.
    private static void ensure(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("Falló una simulación TAC: " + message);
        }
    }

    // source (código de prueba): construye un programa Pascal para cada caso.
    private static String source(String declarations, String statements) {
        return "PROGRAM Demo;\n" + declarations + "\nBEGIN\n" + statements + "\nEND.";
    }

    // parse (analizar): convierte código Pascal en un árbol sintáctico.
    private static ProgramNode parse(String text) {
        return new Parser(new Lexer(text)).parseProgram();
    }

    // compile (compilar): ejecuta el semántico antes del generador TAC.
    private static List<TacInstruction> compile(String text) {
        ProgramNode tree = parse(text);
        SymbolTable table = new SymbolTable(tree);
        new SemanticAnalyzer(table).analyze(tree);
        return new TacGenerator().generate(tree);
    }

    // count (contar): busca cuántos cuádruplos emplean una operación.
    private static int count(List<TacInstruction> instructions, String op) {
        int total = 0;
        for (TacInstruction instruction : instructions) {
            if (instruction.getOp().equals(op)) {
                total++;
            }
        }
        return total;
    }

    // value (leer valor): obtiene una constante o una variable de la simulación.
    private static Object value(String operand, Map<String, Object> variables) {
        if (variables.containsKey(operand)) {
            return variables.get(operand);
        }
        if ("TRUE".equals(operand)) {
            return Boolean.TRUE;
        }
        if ("FALSE".equals(operand)) {
            return Boolean.FALSE;
        }
        if (operand != null && operand.matches("[0-9]+")) {
            return Integer.parseInt(operand);
        }
        if (operand != null && operand.matches("[0-9]+\\.[0-9]+")) {
            return Double.parseDouble(operand);
        }
        throw new RuntimeException("Valor TAC sin definir: " + operand);
    }

    // binary (calcular operación): simula solo las instrucciones para comprobar el flujo.
    // No forma parte del compilador: es una ayuda para estas pruebas.
    private static Object binary(TacInstruction instruction, Map<String, Object> variables) {
        Object left = value(instruction.getArg1(), variables);
        Object right = value(instruction.getArg2(), variables);
        String op = instruction.getOp();

        if (op.equals("==")) {
            if (left instanceof Number && right instanceof Number) {
                return ((Number) left).doubleValue() == ((Number) right).doubleValue();
            }
            return left.equals(right);
        }
        if (op.equals("!=")) {
            if (left instanceof Number && right instanceof Number) {
                return ((Number) left).doubleValue() != ((Number) right).doubleValue();
            }
            return !left.equals(right);
        }
        if (op.equals("<")) return ((Number) left).doubleValue() < ((Number) right).doubleValue();
        if (op.equals("<=")) return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
        if (op.equals(">")) return ((Number) left).doubleValue() > ((Number) right).doubleValue();
        if (op.equals(">=")) return ((Number) left).doubleValue() >= ((Number) right).doubleValue();

        if (instruction.getType() == TokenType.INTEGER) {
            int a = ((Number) left).intValue();
            int b = ((Number) right).intValue();
            switch (op) {
                case "+": return a + b;
                case "-": return a - b;
                case "*": return a * b;
                default: throw new RuntimeException("Operación entera desconocida: " + op);
            }
        }
        double a = ((Number) left).doubleValue();
        double b = ((Number) right).doubleValue();
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new RuntimeException("Se ejecutó una división por cero");
                return a / b;
            default: throw new RuntimeException("Operación desconocida: " + op);
        }
    }

    // simulate (simular): interpreta el TAC únicamente durante las pruebas.
    // Se usa para detectar errores de saltos, ciclos y cortocircuito.
    private static Map<String, Object> simulate(List<TacInstruction> code) {
        Map<String, Integer> labels = new HashMap<>();
        for (int i = 0; i < code.size(); i++) {
            TacInstruction instruction = code.get(i);
            if (instruction.getOp().equals("LABEL")) {
                ensure(!labels.containsKey(instruction.getResult()),
                        "no deben repetirse etiquetas");
                labels.put(instruction.getResult(), i);
            }
        }

        Map<String, Object> variables = new HashMap<>();
        int position = 0;
        int steps = 0;
        while (position < code.size()) {
            if (++steps > 5000) {
                throw new RuntimeException("El TAC no termina: posible error en WHILE");
            }
            TacInstruction instruction = code.get(position);
            String op = instruction.getOp();
            String result = instruction.getResult();
            if (op.equals("LABEL")) {
                position++;
            } else if (op.equals("GOTO")) {
                ensure(labels.containsKey(result), "salto a una etiqueta existente");
                position = labels.get(result);
            } else if (op.equals("IF_TRUE")) {
                Object condition = value(instruction.getArg1(), variables);
                ensure(condition instanceof Boolean, "la condición debe ser BOOLEAN");
                if ((Boolean) condition) {
                    ensure(labels.containsKey(result), "etiqueta de condición existente");
                    position = labels.get(result);
                } else {
                    position++;
                }
            } else {
                Object computed;
                if (op.equals("ASSIGN")) {
                    computed = value(instruction.getArg1(), variables);
                } else if (op.equals("I2R")) {
                    computed = ((Number) value(instruction.getArg1(), variables)).doubleValue();
                } else if (op.equals("NEG")) {
                    Number number = (Number) value(instruction.getArg1(), variables);
                    if (instruction.getType() == TokenType.INTEGER) {
                        computed = -number.intValue();
                    } else {
                        computed = -number.doubleValue();
                    }
                } else {
                    computed = binary(instruction, variables);
                }
                variables.put(result, computed);
                position++;
            }
        }
        return variables;
    }

    // checkAssignments (asignaciones): comprueba operadores, temporales y formato.
    private static void checkAssignments() {
        List<TacInstruction> code = compile(source("VAR x: INTEGER;", "x := 2 + 3 * 4;"));
        check(code.size() == 3, "dos operaciones y una asignación");
        check(code.get(0).getOp().equals("*"), "primero debe calcular multiplicación");
        check(code.get(0).getArg1().equals("3"), "primer operando de multiplicación");
        check(code.get(0).getArg2().equals("4"), "segundo operando de multiplicación");
        check(code.get(0).getResult().equals("_t1"), "primer temporal _t1");
        check(code.get(0).getType() == TokenType.INTEGER, "temporal INTEGER");
        check(code.get(1).toString().equals("_t2 = 2 + _t1"), "formato de suma");
        check(code.get(2).toString().equals("x = _t2"), "formato de asignación");
        check(simulate(code).get("x").equals(14), "resultado final de la expresión");

        List<TacInstruction> empty = compile(source("", ""));
        check(empty.isEmpty(), "los bloques vacíos no generan instrucciones");
    }

    // checkReals (reales): comprueba promociones y división REAL obligatoria.
    private static void checkReals() {
        List<TacInstruction> code = compile(source("VAR r: REAL; b: BOOLEAN;",
                "r := 9 / 2; r := 2 + 3.5; b := r >= 3;"));
        check(count(code, "I2R") == 4, "conversiones reales para división y mezcla");
        check(count(code, "/") == 1, "una división");
        check(count(code, "+") == 1, "una suma");
        check(count(code, ">=") == 1, "una comparación");
        check(code.get(2).getType() == TokenType.REAL, "la división produce REAL");
        Map<String, Object> vars = simulate(code);
        check(vars.get("r").equals(5.5), "suma mixta conserva decimales");
        check(vars.get("b").equals(true), "comparación produce BOOLEAN");

        List<TacInstruction> promoted = compile(source("VAR r: REAL;", "r := 5;"));
        check(count(promoted, "I2R") == 1, "INTEGER a REAL en asignación");
        check(promoted.get(1).getType() == TokenType.REAL, "destino de asignación REAL");
        check(simulate(promoted).get("r").equals(5.0), "conversión preserva el valor");
    }

    // checkUnary (unarios): comprueba los signos y las expresiones con NOT.
    private static void checkUnary() {
        List<TacInstruction> code = compile(source("VAR x: INTEGER; r: REAL; b: BOOLEAN;",
                "x := -5; r := +2.5; b := NOT FALSE;"));
        check(count(code, "NEG") == 1, "menos unario genera NEG");
        check(count(code, "I2R") == 0, "positivo real no necesita conversión");
        check(count(code, "GOTO") >= 2, "NOT se resuelve mediante saltos");
        Map<String, Object> vars = simulate(code);
        check(vars.get("x").equals(-5), "negativo entero correcto");
        check(vars.get("r").equals(2.5), "positivo real correcto");
        check(vars.get("b").equals(true), "NOT FALSE es TRUE");
    }

    // checkConditionals (condiciones): comprueba IF, ELSE y saltos.
    private static void checkConditionals() {
        List<TacInstruction> yes = compile(source("VAR x: INTEGER;",
                "IF 2 < 3 THEN x := 10 ELSE x := 20;"));
        check(count(yes, "IF_TRUE") == 1, "IF usa una condición calculada");
        check(count(yes, "LABEL") == 3, "IF ELSE usa tres etiquetas");
        check(simulate(yes).get("x").equals(10), "se ejecuta THEN");
        List<TacInstruction> no = compile(source("VAR x: INTEGER;",
                "IF 3 < 2 THEN x := 10 ELSE x := 20;"));
        check(simulate(no).get("x").equals(20), "se ejecuta ELSE");
        List<TacInstruction> withoutElse = compile(source("VAR x: INTEGER;",
                "x := 8; IF FALSE THEN x := 10;"));
        check(count(withoutElse, "LABEL") == 2, "IF sin ELSE usa dos etiquetas");
        check(simulate(withoutElse).get("x").equals(8), "IF FALSE no altera x");
    }

    // checkLoops (ciclos): verifica que WHILE recalcule su condición.
    private static void checkLoops() {
        List<TacInstruction> code = compile(source("VAR x: INTEGER;",
                "x := 0; WHILE x < 5 DO x := x + 1;"));
        check(count(code, "LABEL") == 3, "WHILE usa tres etiquetas");
        check(count(code, "GOTO") >= 2, "WHILE vuelve al principio");
        check(simulate(code).get("x").equals(5), "WHILE se detiene en 5");

        List<TacInstruction> none = compile(source("VAR x: INTEGER;",
                "x := 7; WHILE FALSE DO x := x + 1;"));
        check(simulate(none).get("x").equals(7), "WHILE FALSE no ejecuta el cuerpo");

        List<TacInstruction> nested = compile(source("VAR x,y: INTEGER;",
                "x := 0; y := 0; WHILE x < 3 DO BEGIN "
                + "y := 0; WHILE y < 2 DO y := y + 1; x := x + 1; END;"));
        Map<String, Object> vars = simulate(nested);
        check(vars.get("x").equals(3), "ciclo exterior termina");
        check(vars.get("y").equals(2), "ciclo interior termina");
    }

    // checkShortCircuit (cortocircuito): evita evaluar divisiones peligrosas.
    private static void checkShortCircuit() {
        List<TacInstruction> and = compile(source("VAR a: INTEGER; b: BOOLEAN;",
                "a := 0; b := FALSE AND (10 / a > 1);"));
        check(count(and, "IF_TRUE") >= 1, "AND utiliza comprobaciones condicionales");
        check(count(and, "LABEL") >= 4, "AND utiliza etiquetas");
        check(simulate(and).get("b").equals(false), "AND no evalúa su lado derecho");

        List<TacInstruction> or = compile(source("VAR a: INTEGER; b: BOOLEAN;",
                "a := 0; b := TRUE OR (10 / a > 1);"));
        check(simulate(or).get("b").equals(true), "OR no evalúa su lado derecho");

        List<TacInstruction> ifAnd = compile(source("VAR a,x: INTEGER; b: BOOLEAN;",
                "a := 0; x := 0; b := FALSE; "
                + "IF b AND (10 / a > 1) THEN x := 5 ELSE x := 7;"));
        check(simulate(ifAnd).get("x").equals(7), "cortocircuito AND dentro de IF");

        List<TacInstruction> ifOr = compile(source("VAR a,x: INTEGER; b: BOOLEAN;",
                "a := 0; x := 0; b := TRUE; "
                + "IF b OR (10 / a > 1) THEN x := 5 ELSE x := 7;"));
        check(simulate(ifOr).get("x").equals(5), "cortocircuito OR dentro de IF");

        List<TacInstruction> nested = compile(source("VAR a: INTEGER; b: BOOLEAN;",
                "a := 0; b := NOT (TRUE OR (10 / a > 1));"));
        check(simulate(nested).get("b").equals(false), "NOT con OR respeta el cortocircuito");
    }

    // checkBooleans (booleanos): revisa operaciones lógicas y comparaciones.
    private static void checkBooleans() {
        List<TacInstruction> code = compile(source("VAR b,c: BOOLEAN; x: INTEGER;",
                "b := TRUE; c := FALSE; x := 0; "
                + "IF (b = TRUE) AND NOT c THEN x := 1 ELSE x := 2; "
                + "b := (2 < 3) = (4 > 2); c := b <> FALSE;"));
        Map<String, Object> vars = simulate(code);
        check(vars.get("x").equals(1), "combinación de igualdad y NOT");
        check(vars.get("b").equals(true), "igualdad de booleanos");
        check(vars.get("c").equals(true), "desigualdad booleana");
        check(count(code, "==") >= 2, "se generan igualdades");
        check(count(code, "!=") >= 1, "se genera desigualdad");
        check(count(code, "AND") == 0, "AND no se evalúa como operación ansiosa");
        check(count(code, "OR") == 0, "OR no se evalúa como operación ansiosa");
        check(count(code, "NOT") == 0, "NOT usa salto invertido");
    }

    // checkSymbols (símbolos): comprueba nombres y temporales sin colisiones.
    private static void checkSymbols() {
        List<TacInstruction> code = compile(source("VAR Counter, Total: INTEGER;",
                "counter := 1; TOTAL := COUNTER + 2;"));
        check(code.get(0).getResult().equals("Counter"), "nombre original del símbolo destino");
        check(code.get(1).getArg1().equals("Counter"), "identificador enlazado a su símbolo");
        check(code.get(2).getResult().equals("Total"), "segunda asignación conserva mayúsculas");
        check(simulate(code).get("Total").equals(3), "variables sin distinguir mayúsculas");
        Set<String> temps = new HashSet<>();
        for (TacInstruction instruction : code) {
            if (instruction.getResult() != null && instruction.getResult().startsWith("_t")) {
                check(temps.add(instruction.getResult()), "el temporal se define una vez");
            }
        }
    }

    // checkGeneratorState (estado): comprueba reinicio, copias y exportación.
    private static void checkGeneratorState() throws IOException {
        ProgramNode tree = parse(source("VAR x: INTEGER;", "x := 1 + 2;"));
        new SemanticAnalyzer(new SymbolTable(tree)).analyze(tree);
        TacGenerator generator = new TacGenerator();
        List<TacInstruction> first = generator.generate(tree);
        List<TacInstruction> second = generator.generate(tree);
        check(first.size() == second.size(), "generate reinicia la lista");
        check(first.toString().equals(second.toString()), "generate reinicia los temporales");
        second.clear();
        check(generator.getCode().size() == first.size(), "getCode entrega una copia");
        check(generator.getText().contains("_t1 = 1 + 2"), "getText presenta las instrucciones");
        Path temp = Files.createTempFile("pascal-subset-tac-", ".txt");
        try {
            generator.writeFile(temp);
            check(Files.readString(temp).equals(generator.getText()), "writeFile conserva el texto");
        } finally {
            Files.deleteIfExists(temp);
        }
        ProgramNode untyped = parse(source("VAR x: INTEGER;", "x := 1;"));
        boolean refused = false;
        try {
            new TacGenerator().generate(untyped);
        } catch (IllegalStateException ex) {
            refused = ex.getMessage().contains("análisis semántico");
        }
        check(refused, "no genera TAC sin decorar el árbol");
    }

    // checkAllLabels (etiquetas): comprueba destinos de saltos y tipos.
    private static void checkAllLabels() {
        List<TacInstruction> code = compile(source("VAR x: INTEGER; b: BOOLEAN;",
                "x := 0; b := TRUE; WHILE b AND (x < 4) DO BEGIN "
                + "IF x = 2 THEN b := FALSE ELSE x := x + 1; END;"));
        Set<String> labels = new HashSet<>();
        Set<String> targets = new HashSet<>();
        for (TacInstruction instruction : code) {
            String op = instruction.getOp();
            if (op.equals("LABEL")) labels.add(instruction.getResult());
            if (op.equals("GOTO") || op.equals("IF_TRUE")) {
                targets.add(instruction.getResult());
                check(instruction.getType() == null, "saltos sin tipo numérico");
            }
        }
        check(labels.containsAll(targets), "todos los destinos de salto existen");
        check(simulate(code).get("x").equals(2), "flujo combinado IF y WHILE correcto");
    }

    // main (ejecutar pruebas): revisa el generador y los casos anteriores.
    public static void main(String[] args) throws IOException {
        checkAssignments();
        checkReals();
        checkUnary();
        checkConditionals();
        checkLoops();
        checkShortCircuit();
        checkBooleans();
        checkSymbols();
        checkGeneratorState();
        checkAllLabels();
        System.out.println("Pruebas del generador TAC superadas: " + passed);
    }
}
