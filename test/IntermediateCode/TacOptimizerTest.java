package IntermediateCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Lexer.Lexer;
import Lexer.TokenType;
import Parser.Parser;
import Parser.ProgramNode;
import Semantic.SemanticAnalyzer;
import SymbolTable.SymbolTable;

// TacOptimizerTest (pruebas de optimización): verifica reglas y comportamiento.
// Incluye un intérprete pequeño solo para comparar el TAC original y optimizado.
public class TacOptimizerTest {
    private static int passed = 0;

    // check (comprobar): cuenta las verificaciones correctas.
    private static void check(boolean condition, String description) {
        if (!condition) throw new RuntimeException("Falló la prueba del optimizador: " + description);
        passed++;
    }

    // q (cuádruplo): facilita la construcción de instrucciones para las pruebas.
    private static TacInstruction q(String op, String a, String b, String out, TokenType type) {
        return new TacInstruction(op, a, b, out, type);
    }

    // code (lista TAC): construye ejemplos pequeños independientes del parser.
    private static List<TacInstruction> code(TacInstruction... items) {
        List<TacInstruction> result = new ArrayList<>();
        for (TacInstruction item : items) result.add(item);
        return result;
    }

    // optimize (optimizar): ejecuta el optimizador con su lista de entrada.
    private static List<TacInstruction> optimize(List<TacInstruction> input) {
        return new TacOptimizer().optimize(input);
    }

    // generated (generar): ejecuta todas las fases desde un programa Pascal.
    private static List<TacInstruction> generated(String declarations, String statements) {
        String input = "PROGRAM Demo;\n" + declarations + "\nBEGIN\n" + statements + "\nEND.";
        ProgramNode tree = new Parser(new Lexer(input)).parseProgram();
        SymbolTable table = new SymbolTable(tree);
        new SemanticAnalyzer(table).analyze(tree);
        return new TacGenerator().generate(tree);
    }

    // count (contar): devuelve las instrucciones de una operación concreta.
    private static int count(List<TacInstruction> instructions, String op) {
        int value = 0;
        for (TacInstruction item : instructions) if (op.equals(item.getOp())) value++;
        return value;
    }

    // has (contiene): busca un cuádruplo por destino y operación.
    private static boolean has(List<TacInstruction> instructions, String op, String result) {
        for (TacInstruction item : instructions) {
            if (op.equals(item.getOp()) && result.equals(item.getResult())) return true;
        }
        return false;
    }

    // read (leer): obtiene literales o variables para la simulación.
    private static Object read(String operand, Map<String, Object> values) {
        if (values.containsKey(operand)) return values.get(operand);
        if ("TRUE".equals(operand)) return Boolean.TRUE;
        if ("FALSE".equals(operand)) return Boolean.FALSE;
        if (operand != null && operand.matches("-?[0-9]+")) return Integer.parseInt(operand);
        if (operand != null && operand.matches("-?[0-9]+\\.[0-9]+(?:[Ee][+-]?[0-9]+)?")) {
            return Double.parseDouble(operand);
        }
        throw new RuntimeException("Operando no definido: " + operand);
    }

    // calculate (calcular): evalúa una instrucción TAC en las pruebas.
    private static Object calculate(TacInstruction item, Map<String, Object> values) {
        String op = item.getOp();
        Object a = read(item.getArg1(), values);
        if ("ASSIGN".equals(op)) return a;
        if ("I2R".equals(op)) return ((Number) a).doubleValue();
        if ("NEG".equals(op)) {
            if (item.getType() == TokenType.INTEGER) return -((Number) a).intValue();
            return -((Number) a).doubleValue();
        }
        Object b = read(item.getArg2(), values);
        if ("==".equals(op) || "!=".equals(op)) {
            boolean equal;
            if (a instanceof Number && b instanceof Number) {
                equal = ((Number) a).doubleValue() == ((Number) b).doubleValue();
            } else {
                equal = a.equals(b);
            }
            return "==".equals(op) ? equal : !equal;
        }
        if ("<".equals(op)) return ((Number) a).doubleValue() < ((Number) b).doubleValue();
        if ("<=".equals(op)) return ((Number) a).doubleValue() <= ((Number) b).doubleValue();
        if (">".equals(op)) return ((Number) a).doubleValue() > ((Number) b).doubleValue();
        if (">=".equals(op)) return ((Number) a).doubleValue() >= ((Number) b).doubleValue();
        if (item.getType() == TokenType.INTEGER) {
            int left = ((Number) a).intValue();
            int right = ((Number) b).intValue();
            switch (op) {
                case "+": return left + right;
                case "-": return left - right;
                case "*": return left * right;
            }
        }
        double left = ((Number) a).doubleValue();
        double right = ((Number) b).doubleValue();
        switch (op) {
            case "+": return left + right;
            case "-": return left - right;
            case "*": return left * right;
            case "/":
                if (right == 0.0) throw new RuntimeException("Se ejecutó una división por cero");
                return left / right;
            default: throw new RuntimeException("Operación TAC desconocida: " + op);
        }
    }

    // simulate (simular): sigue etiquetas y saltos, con límite para detectar ciclos.
    private static Map<String, Object> simulate(List<TacInstruction> instructions) {
        Map<String, Integer> labels = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            if ("LABEL".equals(instructions.get(i).getOp())) {
                String name = instructions.get(i).getResult();
                if (labels.put(name, i) != null) throw new RuntimeException("Etiqueta duplicada: " + name);
            }
        }
        Map<String, Object> values = new HashMap<>();
        int index = 0;
        int steps = 0;
        while (index < instructions.size()) {
            if (++steps > 10000) throw new RuntimeException("El TAC no termina");
            TacInstruction item = instructions.get(index);
            String op = item.getOp();
            if ("LABEL".equals(op)) {
                index++;
            } else if ("GOTO".equals(op)) {
                Integer target = labels.get(item.getResult());
                if (target == null) throw new RuntimeException("Etiqueta ausente: " + item.getResult());
                index = target;
            } else if ("IF_TRUE".equals(op)) {
                if ((Boolean) read(item.getArg1(), values)) {
                    Integer target = labels.get(item.getResult());
                    if (target == null) throw new RuntimeException("Etiqueta ausente: " + item.getResult());
                    index = target;
                } else {
                    index++;
                }
            } else {
                values.put(item.getResult(), calculate(item, values));
                index++;
            }
        }
        return values;
    }

    // equivalent (equivalente): compara los valores finales de variables Pascal.
    private static void equivalent(List<TacInstruction> original, String... variables) {
        List<TacInstruction> optimized = optimize(original);
        Map<String, Object> before = simulate(original);
        Map<String, Object> after = simulate(optimized);
        for (String variable : variables) {
            Object a = before.get(variable);
            Object b = after.get(variable);
            if (a instanceof Double && b instanceof Double) {
                check(Double.doubleToRawLongBits((Double) a) == Double.doubleToRawLongBits((Double) b),
                        "valor REAL de " + variable + " debe conservar incluso el signo de cero");
            } else {
                check(a != null && a.equals(b), "el valor final de " + variable + " debe ser igual");
            }
        }
    }

    // checkIntegerFolding (probar enteros): valida plegado y límites de 32 bits.
    private static void checkIntegerFolding() {
        List<TacInstruction> original = code(
                q("*", "2", "3", "_t1", TokenType.INTEGER),
                q("+", "_t1", "5", "_t2", TokenType.INTEGER),
                q("ASSIGN", "_t2", null, "x", TokenType.INTEGER));
        List<TacInstruction> after = optimize(original);
        check(count(after, "*") == 0, "2*3 plegado");
        check(count(after, "+") == 0, "6+5 plegado");
        check(after.size() < original.size(), "temporales muertos eliminados");
        check("ASSIGN".equals(after.get(after.size() - 1).getOp()), "se conserva asignación final");
        check("11".equals(after.get(after.size() - 1).getArg1()), "resultado 11");
        equivalent(original, "x");

        List<TacInstruction> neg = code(q("NEG", "7", null, "_t1", TokenType.INTEGER),
                q("ASSIGN", "_t1", null, "x", TokenType.INTEGER));
        check("-7".equals(optimize(neg).get(0).getArg1()), "plegado de NEG entero");
        equivalent(neg, "x");

        List<TacInstruction> overflow = code(q("+", "2147483647", "1", "_t1", TokenType.INTEGER),
                q("ASSIGN", "_t1", null, "x", TokenType.INTEGER));
        check(count(optimize(overflow), "+") == 1, "no plegar overflow INTEGER");
        check(count(overflow, "+") == 1, "la lista original no cambia");
    }

    // checkRealFolding (probar reales): valida división real y conversiones I2R.
    private static void checkRealFolding() {
        List<TacInstruction> mixed = generated("VAR r: REAL;", "r := 9 / 2;");
        List<TacInstruction> after = optimize(mixed);
        check(count(after, "I2R") == 0, "I2R constante plegada");
        check(count(after, "/") == 0, "9 / 2 se calcula como REAL");
        check(after.stream().anyMatch(x -> "ASSIGN".equals(x.getOp()) && "4.5".equals(x.getArg1())),
                "resultado 4.5, no división entera");
        equivalent(mixed, "r");

        List<TacInstruction> sum = code(q("+", "2.5", "3.25", "_t1", TokenType.REAL),
                q("ASSIGN", "_t1", null, "r", TokenType.REAL));
        check("5.75".equals(optimize(sum).get(0).getArg1()), "suma REAL plegada");
        equivalent(sum, "r");

        List<TacInstruction> neg = code(q("NEG", "0.0", null, "_t1", TokenType.REAL),
                q("ASSIGN", "_t1", null, "r", TokenType.REAL));
        check("-0.0".equals(optimize(neg).get(0).getArg1()), "NEG conserva -0.0");
        equivalent(neg, "r");
    }

    // checkAlgebraic (probar identidades): distingue reglas válidas por tipo.
    private static void checkAlgebraic() {
        String[] operations = {"+", "+", "-", "*", "*", "*", "*"};
        String[] left = {"x", "0", "x", "x", "1", "x", "0"};
        String[] right = {"0", "x", "0", "1", "x", "0", "x"};
        String[] expected = {"x", "x", "x", "x", "x", "0", "0"};
        for (int i = 0; i < operations.length; i++) {
            List<TacInstruction> input = code(q(operations[i], left[i], right[i], "_t1", TokenType.INTEGER),
                    q("ASSIGN", "_t1", null, "y", TokenType.INTEGER));
            List<TacInstruction> result = optimize(input);
            String expectedOperand = expected[i];
            check(count(result, operations[i]) == 0, "identidad entera " + i);
            check(result.stream().anyMatch(x -> ("y".equals(x.getResult()) || "_t1".equals(x.getResult())) && expectedOperand.equals(x.getArg1())),
                    "operando esperado en identidad " + i);
        }
        List<TacInstruction> realZero = code(q("*", "r", "0.0", "_t1", TokenType.REAL),
                q("ASSIGN", "_t1", null, "z", TokenType.REAL));
        check(count(optimize(realZero), "*") == 1, "no convertir REAL*0 en cero");
        List<TacInstruction> realAddZero = code(q("+", "r", "0.0", "_t1", TokenType.REAL),
                q("ASSIGN", "_t1", null, "z", TokenType.REAL));
        check(count(optimize(realAddZero), "+") == 1, "no cambiar signo de cero con REAL+0");
        List<TacInstruction> realMulOne = code(q("*", "r", "1.0", "_t1", TokenType.REAL),
                q("ASSIGN", "_t1", null, "z", TokenType.REAL));
        check(count(optimize(realMulOne), "*") == 0, "REAL*1 se simplifica");
        List<TacInstruction> realDivOne = code(q("/", "r", "1.0", "_t1", TokenType.REAL),
                q("ASSIGN", "_t1", null, "z", TokenType.REAL));
        check(count(optimize(realDivOne), "/") == 0, "REAL/1 se simplifica");
    }

    // checkComparisons (probar comparaciones): pliega relacionales y booleanos.
    private static void checkComparisons() {
        String[] ops = {"==", "!=", "<", "<=", ">", ">="};
        String[] left = {"TRUE", "TRUE", "2", "2.5", "3", "3.0"};
        String[] right = {"TRUE", "FALSE", "3", "2.5", "2", "3.0"};
        for (int i = 0; i < ops.length; i++) {
            List<TacInstruction> input = code(q(ops[i], left[i], right[i], "_t1", TokenType.BOOLEAN),
                    q("ASSIGN", "_t1", null, "b", TokenType.BOOLEAN));
            List<TacInstruction> result = optimize(input);
            check(count(result, ops[i]) == 0, "comparación plegada " + ops[i]);
            check(result.stream().anyMatch(x -> "b".equals(x.getResult()) && "TRUE".equals(x.getArg1())),
                    "comparación TRUE " + ops[i]);
            equivalent(input, "b");
        }
    }

    // checkPropagation (probar propagación): respeta cambios de variables.
    private static void checkPropagation() {
        List<TacInstruction> source = code(q("ASSIGN", "1", null, "x", TokenType.INTEGER),
                q("ASSIGN", "x", null, "y", TokenType.INTEGER),
                q("ASSIGN", "2", null, "x", TokenType.INTEGER),
                q("ASSIGN", "y", null, "z", TokenType.INTEGER));
        List<TacInstruction> out = optimize(source);
        check(out.stream().anyMatch(x -> "z".equals(x.getResult()) && "1".equals(x.getArg1())),
                "la copia y conserva el valor previo de x");
        equivalent(source, "x", "y", "z");
        List<TacInstruction> repeated = optimize(out);
        check(out.size() == repeated.size(), "optimizar dos veces no cambia cantidad de instrucciones");
        check(out.toString().equals(repeated.toString()), "optimizar dos veces es idempotente");
    }

    // checkDeadCode (probar código muerto): preserva salidas y errores potenciales.
    private static void checkDeadCode() {
        List<TacInstruction> source = code(q("+", "2", "3", "_t1", TokenType.INTEGER),
                q("*", "4", "5", "_t2", TokenType.INTEGER),
                q("ASSIGN", "_t1", null, "x", TokenType.INTEGER),
                q("ASSIGN", "40", null, "z", TokenType.INTEGER));
        List<TacInstruction> out = optimize(source);
        check(count(out, "+") == 0, "expresión plegada");
        check(!has(out, "ASSIGN", "_t2"), "temporal sin uso eliminado");
        check(has(out, "ASSIGN", "x"), "variable final x conservada");
        check(has(out, "ASSIGN", "z"), "variable final z conservada aunque no se lea");
        equivalent(source, "x", "z");

        List<TacInstruction> risky = code(q("/", "5.0", "0.0", "_t1", TokenType.REAL),
                q("ASSIGN", "1", null, "x", TokenType.INTEGER));
        check(count(optimize(risky), "/") == 1, "división por cero potencial no eliminada");
        List<TacInstruction> unknown = code(q("/", "a", "b", "_t1", TokenType.REAL),
                q("ASSIGN", "1", null, "x", TokenType.INTEGER));
        check(count(optimize(unknown), "/") == 1, "división con denominador variable no eliminada");
        List<TacInstruction> harmless = code(q("+", "a", "b", "_t1", TokenType.INTEGER),
                q("ASSIGN", "1", null, "x", TokenType.INTEGER));
        check(count(optimize(harmless), "+") == 0, "cálculo puro no utilizado eliminado");
    }

    // checkFlow (probar flujo): elimina ramas inalcanzables sin romper saltos.
    private static void checkFlow() {
        List<TacInstruction> direct = code(q("GOTO", null, null, "L1", null),
                q("ASSIGN", "99", null, "x", TokenType.INTEGER),
                q("LABEL", null, null, "L1", null),
                q("ASSIGN", "1", null, "x", TokenType.INTEGER));
        List<TacInstruction> out = optimize(direct);
        check(out.size() < direct.size(), "camino después de GOTO eliminado");
        check(out.stream().noneMatch(x -> "99".equals(x.getArg1())), "asignación inalcanzable eliminada");
        equivalent(direct, "x");
        List<TacInstruction> falseBranch = code(q("IF_TRUE", "FALSE", null, "L1", null),
                q("ASSIGN", "1", null, "x", TokenType.INTEGER),
                q("GOTO", null, null, "L2", null),
                q("LABEL", null, null, "L1", null),
                q("ASSIGN", "2", null, "x", TokenType.INTEGER),
                q("LABEL", null, null, "L2", null));
        out = optimize(falseBranch);
        check(count(out, "IF_TRUE") == 0, "IF falso simplificado");
        check(out.stream().noneMatch(x -> "2".equals(x.getArg1())), "rama verdadera inalcanzable eliminada");
        equivalent(falseBranch, "x");
        List<TacInstruction> trueBranch = code(q("IF_TRUE", "TRUE", null, "L1", null),
                q("ASSIGN", "1", null, "x", TokenType.INTEGER),
                q("LABEL", null, null, "L1", null),
                q("ASSIGN", "2", null, "x", TokenType.INTEGER));
        out = optimize(trueBranch);
        check(count(out, "IF_TRUE") == 0, "IF verdadero convertido en salto");
        equivalent(trueBranch, "x");
    }

    // checkPrograms (probar programas): compara ciclos, condicionales y booleanos.
    private static void checkPrograms() {
        List<TacInstruction> loop = generated("VAR x: INTEGER;", "x := 0; WHILE x < 5 DO x := x + 1;");
        equivalent(loop, "x");
        check(simulate(optimize(loop)).get("x").equals(5), "WHILE llega a cinco");
        check(count(optimize(loop), "IF_TRUE") > 0, "conserva condición de WHILE");
        List<TacInstruction> nested = generated("VAR x: INTEGER; b: BOOLEAN;",
                "x := 0; b := TRUE; WHILE (x < 4) AND b DO BEGIN x := x + 1; IF x = 4 THEN b := FALSE; END;");
        equivalent(nested, "x", "b");
        check(simulate(optimize(nested)).get("x").equals(4), "WHILE anidado produce cuatro");
        List<TacInstruction> ifElse = generated("VAR x: INTEGER; b: BOOLEAN;",
                "b := FALSE; IF b THEN x := 10 ELSE x := 20;");
        equivalent(ifElse, "b", "x");
        check(simulate(optimize(ifElse)).get("x").equals(20), "ELSE elegido correctamente");
        List<TacInstruction> ifNoElse = generated("VAR x: INTEGER; b: BOOLEAN;",
                "x := 3; b := FALSE; IF b THEN x := 5; x := x + 1;");
        equivalent(ifNoElse, "x", "b");
        check(simulate(optimize(ifNoElse)).get("x").equals(4), "IF sin ELSE respeta continuidad");
        List<TacInstruction> logic = generated("VAR b: BOOLEAN;",
                "b := FALSE AND (1 / 0 > 1);");
        equivalent(logic, "b");
        check(simulate(optimize(logic)).get("b").equals(false), "AND cortocircuita división por cero");
        List<TacInstruction> logic2 = generated("VAR b: BOOLEAN;",
                "b := TRUE OR (1 / 0 > 1);");
        equivalent(logic2, "b");
        check(simulate(optimize(logic2)).get("b").equals(true), "OR cortocircuita división por cero");
        List<TacInstruction> not = generated("VAR b: BOOLEAN;", "b := NOT FALSE;");
        equivalent(not, "b");
        check(simulate(optimize(not)).get("b").equals(true), "NOT conserva verdad");
        List<TacInstruction> real = generated("VAR r: REAL; x: INTEGER;",
                "x := 3; r := x + 2.5; r := r * 2;");
        equivalent(real, "x", "r");
        check(simulate(optimize(real)).get("r").equals(11.0), "conversiones REAL correctas");
    }

    // main (ejecutar pruebas): revisa cada técnica y la equivalencia observable.
    public static void main(String[] args) {
        checkIntegerFolding();
        checkRealFolding();
        checkAlgebraic();
        checkComparisons();
        checkPropagation();
        checkDeadCode();
        checkFlow();
        checkPrograms();
        System.out.println("Pruebas de optimización TAC superadas: " + passed);
    }
}
