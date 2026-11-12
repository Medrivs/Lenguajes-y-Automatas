package IntermediateCode;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import Lexer.TokenType;

// TacOptimizer (optimizador TAC): simplifica cuádruplos sin alterar el flujo.
// No modifica la lista original; devuelve una lista nueva de instrucciones.
public class TacOptimizer {

    // optimize (optimizar): repite las pasadas hasta que el TAC deje de cambiar.
    public List<TacInstruction> optimize(List<TacInstruction> original) {
        List<TacInstruction> current = new ArrayList<>(original);
        boolean changed;
        do {
            List<TacInstruction> next = simplify(current);
            next = removeUnreachable(next);
            next = removeUnusedTemporaries(next);
            next = removeRedundantJumps(next);
            next = removeUnreachable(next);
            changed = !sameCode(current, next);
            current = next;
        } while (changed);
        return current;
    }

    // sameCode (comparar): revisa los cinco campos de cada cuádruplo.
    private boolean sameCode(List<TacInstruction> first, List<TacInstruction> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            TacInstruction a = first.get(i);
            TacInstruction b = second.get(i);
            if (!equal(a.getOp(), b.getOp()) || !equal(a.getArg1(), b.getArg1()) ||
                    !equal(a.getArg2(), b.getArg2()) || !equal(a.getResult(), b.getResult()) ||
                    a.getType() != b.getType()) return false;
        }
        return true;
    }

    // equal (comparar texto): admite valores null sin producir excepciones.
    private boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    // instruction (construir): conserva la representación tipada del TAC.
    private TacInstruction instruction(String op, String a, String b, String result, TokenType type) {
        return new TacInstruction(op, a, b, result, type);
    }

    // assignment (asignar): sustituye una operación por una asignación segura.
    private TacInstruction assignment(String value, TacInstruction original) {
        return instruction("ASSIGN", value, null, original.getResult(), original.getType());
    }

    // integerValue (leer entero): reconoce enteros con signo y ceros iniciales.
    private BigInteger integerValue(String text) {
        if (text == null || !text.matches("-?[0-9]+")) return null;
        try {
            return new BigInteger(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // realValue (leer real): reconoce constantes REAL finitas en el TAC.
    private Double realValue(String text) {
        if (text == null || !text.matches("-?(?:[0-9]+\\.[0-9]*|[0-9]+(?:\\.[0-9]*)?[Ee][+-]?[0-9]+)")) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // isConstant (es constante): detecta enteros, reales y booleanos.
    private boolean isConstant(String text) {
        return "TRUE".equals(text) || "FALSE".equals(text) ||
                integerValue(text) != null || realValue(text) != null;
    }

    // resolve (resolver): sustituye una variable por una constante conocida.
    private String resolve(String text, Map<String, String> constants) {
        if (text == null || isConstant(text)) return text;
        return constants.getOrDefault(text, text);
    }

    // remember (recordar): actualiza la constante del destino tras escribirlo.
    private void remember(Map<String, String> constants, TacInstruction item) {
        String destination = item.getResult();
        if (destination == null) return;
        constants.remove(destination);
        if ("ASSIGN".equals(item.getOp()) && isConstant(item.getArg1())) {
            constants.put(destination, item.getArg1());
        }
    }

    // simplify (simplificar): trabaja dentro de bloques sin cruzar saltos o etiquetas.
    // Esto impide trasladar constantes entre ramas o iteraciones de WHILE.
    private List<TacInstruction> simplify(List<TacInstruction> source) {
        List<TacInstruction> result = new ArrayList<>();
        Map<String, String> constants = new HashMap<>();
        for (TacInstruction item : source) {
            String op = item.getOp();
            if ("LABEL".equals(op) || "GOTO".equals(op)) {
                result.add(item);
                constants.clear();
                continue;
            }
            String left = resolve(item.getArg1(), constants);
            String right = resolve(item.getArg2(), constants);
            if ("IF_TRUE".equals(op)) {
                if ("TRUE".equals(left)) {
                    result.add(instruction("GOTO", null, null, item.getResult(), null));
                } else if (!"FALSE".equals(left)) {
                    result.add(instruction("IF_TRUE", left, null, item.getResult(), null));
                }
                constants.clear();
                continue;
            }
            TacInstruction next = instruction(op, left, right, item.getResult(), item.getType());
            if ("I2R".equals(op)) {
                BigInteger value = integerValue(left);
                if (value != null && fitsInteger(value)) {
                    next = assignment(Double.toString(value.doubleValue()), item);
                }
            } else if ("NEG".equals(op)) {
                String folded = negate(left, item.getType());
                if (folded != null) next = assignment(folded, item);
            } else if (isBinary(op)) {
                String folded = fold(op, left, right, item.getType());
                if (folded != null) {
                    next = assignment(folded, item);
                } else {
                    String simple = algebraic(op, left, right, item.getType());
                    if (simple != null) next = assignment(simple, item);
                }
            }
            result.add(next);
            remember(constants, next);
        }
        return result;
    }

    // fitsInteger (validar entero): nunca pliega resultados fuera de 32 bits.
    private boolean fitsInteger(BigInteger n) {
        return n.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 &&
                n.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    // negate (negar): calcula un signo negativo solo cuando es seguro.
    private String negate(String text, TokenType type) {
        if (type == TokenType.INTEGER) {
            BigInteger n = integerValue(text);
            if (n == null || !fitsInteger(n.negate())) return null;
            return n.negate().toString();
        }
        if (type == TokenType.REAL) {
            Double n = realValue(text);
            if (n == null) return null;
            return Double.toString(-n);
        }
        return null;
    }

    // isBinary (es binaria): distingue operaciones de cálculo y comparación.
    private boolean isBinary(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op) ||
                "/".equals(op) || "==".equals(op) || "!=".equals(op) ||
                "<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op);
    }

    // fold (plegar): evalúa operaciones entre constantes sin dividir entre cero.
    private String fold(String op, String left, String right, TokenType type) {
        if (!isConstant(left) || !isConstant(right)) return null;
        if ("==".equals(op) || "!=".equals(op) || "<".equals(op) ||
                "<=".equals(op) || ">".equals(op) || ">=".equals(op)) {
            boolean answer;
            if (("TRUE".equals(left) || "FALSE".equals(left)) &&
                    ("TRUE".equals(right) || "FALSE".equals(right))) {
                if (!"==".equals(op) && !"!=".equals(op)) return null;
                answer = left.equals(right);
                if ("!=".equals(op)) answer = !answer;
            } else {
                Double a = number(left);
                Double b = number(right);
                if (a == null || b == null) return null;
                switch (op) {
                    case "==": answer = a.doubleValue() == b.doubleValue(); break;
                    case "!=": answer = a.doubleValue() != b.doubleValue(); break;
                    case "<": answer = a < b; break;
                    case "<=": answer = a <= b; break;
                    case ">": answer = a > b; break;
                    default: answer = a >= b; break;
                }
            }
            return answer ? "TRUE" : "FALSE";
        }
        if (type == TokenType.INTEGER) {
            BigInteger a = integerValue(left);
            BigInteger b = integerValue(right);
            if (a == null || b == null) return null;
            BigInteger value;
            switch (op) {
                case "+": value = a.add(b); break;
                case "-": value = a.subtract(b); break;
                case "*": value = a.multiply(b); break;
                default: return null;
            }
            return fitsInteger(value) ? value.toString() : null;
        }
        if (type == TokenType.REAL) {
            Double a = number(left);
            Double b = number(right);
            if (a == null || b == null) return null;
            if ("/".equals(op) && b == 0.0) return null;
            double value;
            switch (op) {
                case "+": value = a + b; break;
                case "-": value = a - b; break;
                case "*": value = a * b; break;
                case "/": value = a / b; break;
                default: return null;
            }
            return Double.isFinite(value) ? Double.toString(value) : null;
        }
        return null;
    }

    // number (leer número): convierte enteros o reales a double cuando hace falta.
    private Double number(String value) {
        BigInteger integer = integerValue(value);
        if (integer != null) return fitsInteger(integer) ? integer.doubleValue() : null;
        return realValue(value);
    }

    // zero (es cero): se usa solo para identidades enteras seguras.
    private boolean zero(String value) {
        BigInteger integer = integerValue(value);
        return integer != null && integer.signum() == 0;
    }

    // one (es uno): reconoce la identidad multiplicativa del tipo adecuado.
    private boolean one(String value, TokenType type) {
        if (type == TokenType.INTEGER) {
            BigInteger integer = integerValue(value);
            return integer != null && integer.equals(BigInteger.ONE);
        }
        Double real = realValue(value);
        return type == TokenType.REAL && real != null && real == 1.0;
    }

    // algebraic (simplificación): aplica identidades sin cambiar tipos ni excepciones.
    // No usamos x*0 para REAL: podría ocultar NaN o una división no finita.
    private String algebraic(String op, String a, String b, TokenType type) {
        if (type == TokenType.INTEGER) {
            if ("+".equals(op) && zero(a)) return b;
            if ("+".equals(op) && zero(b)) return a;
            if ("-".equals(op) && zero(b)) return a;
            if ("*".equals(op) && (zero(a) || zero(b))) return "0";
            if ("*".equals(op) && one(a, type)) return b;
            if ("*".equals(op) && one(b, type)) return a;
        }
        if (type == TokenType.REAL) {
            if ("*".equals(op) && one(a, type)) return b;
            if ("*".equals(op) && one(b, type)) return a;
            if ("/".equals(op) && one(b, type)) return a;
        }
        return null;
    }

    // labels (ubicar etiquetas): relaciona el nombre con su posición en el TAC.
    private Map<String, Integer> labels(List<TacInstruction> code) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < code.size(); i++) {
            if ("LABEL".equals(code.get(i).getOp())) result.put(code.get(i).getResult(), i);
        }
        return result;
    }

    // successors (sucesores): construye los caminos reales del flujo de control.
    private List<Integer> successors(List<TacInstruction> code, int i, Map<String, Integer> labels) {
        List<Integer> next = new ArrayList<>();
        TacInstruction item = code.get(i);
        if ("GOTO".equals(item.getOp())) {
            Integer target = labels.get(item.getResult());
            if (target != null) next.add(target);
        } else {
            if ("IF_TRUE".equals(item.getOp())) {
                Integer target = labels.get(item.getResult());
                if (target != null) next.add(target);
            }
            if (i + 1 < code.size()) next.add(i + 1);
        }
        return next;
    }

    // removeUnreachable (eliminar inalcanzable): descarta código sin ruta desde el inicio.
    private List<TacInstruction> removeUnreachable(List<TacInstruction> code) {
        if (code.isEmpty()) return new ArrayList<>();
        Map<String, Integer> targets = labels(code);
        Set<Integer> reached = new HashSet<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        pending.add(0);
        while (!pending.isEmpty()) {
            int index = pending.removeFirst();
            if (!reached.add(index)) continue;
            for (int successor : successors(code, index, targets)) {
                if (!reached.contains(successor)) pending.add(successor);
            }
        }
        List<TacInstruction> result = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            if (reached.contains(i)) result.add(code.get(i));
        }
        return result;
    }

    // uses (usos): obtiene las variables leídas por una instrucción.
    private Set<String> uses(TacInstruction item) {
        Set<String> result = new HashSet<>();
        String op = item.getOp();
        if ("LABEL".equals(op) || "GOTO".equals(op)) return result;
        if (isVariable(item.getArg1())) result.add(item.getArg1());
        if (!"IF_TRUE".equals(op) && isVariable(item.getArg2())) result.add(item.getArg2());
        return result;
    }

    // isVariable (es variable): separa nombres de literales y valores nulos.
    private boolean isVariable(String value) {
        return value != null && !isConstant(value);
    }

    // isTemporary (es temporal): los identificadores Pascal no empiezan con '_'.
    private boolean isTemporary(String result) {
        return result != null && result.startsWith("_t");
    }

    // writesResult (es escritura): distingue los cálculos de saltos y etiquetas.
    private boolean writesResult(TacInstruction item) {
        return !"LABEL".equals(item.getOp()) && !"GOTO".equals(item.getOp()) &&
                !"IF_TRUE".equals(item.getOp());
    }

    // safeToRemove (se puede eliminar): conserva divisiones con posibles errores.
    private boolean safeToRemove(TacInstruction item) {
        if (!writesResult(item)) return false;
        if (!"/".equals(item.getOp())) return true;
        Double divisor = number(item.getArg2());
        return divisor != null && divisor != 0.0;
    }

    // removeUnusedTemporaries (eliminar temporales): analiza usos en todas las rutas.
    // La vivacidad considera ciclos y bifurcaciones, no solo el orden de líneas.
    private List<TacInstruction> removeUnusedTemporaries(List<TacInstruction> code) {
        int length = code.size();
        Map<String, Integer> targets = labels(code);
        List<Set<String>> liveIn = new ArrayList<>();
        List<Set<String>> liveOut = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            liveIn.add(new HashSet<>());
            liveOut.add(new HashSet<>());
        }
        boolean changed;
        do {
            changed = false;
            for (int i = length - 1; i >= 0; i--) {
                Set<String> out = new HashSet<>();
                for (int successor : successors(code, i, targets)) out.addAll(liveIn.get(successor));
                Set<String> in = new HashSet<>(out);
                TacInstruction item = code.get(i);
                if (writesResult(item)) in.remove(item.getResult());
                in.addAll(uses(item));
                if (!out.equals(liveOut.get(i)) || !in.equals(liveIn.get(i))) {
                    liveOut.set(i, out);
                    liveIn.set(i, in);
                    changed = true;
                }
            }
        } while (changed);
        List<TacInstruction> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            TacInstruction item = code.get(i);
            if (isTemporary(item.getResult()) && writesResult(item) &&
                    !liveOut.get(i).contains(item.getResult()) && safeToRemove(item)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    // removeRedundantJumps (eliminar saltos sobrantes): conserva etiquetas usadas.
    private List<TacInstruction> removeRedundantJumps(List<TacInstruction> code) {
        List<TacInstruction> result = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            TacInstruction item = code.get(i);
            if ("GOTO".equals(item.getOp()) && i + 1 < code.size()) {
                TacInstruction next = code.get(i + 1);
                if ("LABEL".equals(next.getOp()) && equal(item.getResult(), next.getResult())) {
                    continue;
                }
            }
            result.add(item);
        }
        Set<String> referenced = new HashSet<>();
        for (TacInstruction item : result) {
            if ("GOTO".equals(item.getOp()) || "IF_TRUE".equals(item.getOp())) {
                referenced.add(item.getResult());
            }
        }
        List<TacInstruction> withoutOrphans = new ArrayList<>();
        for (TacInstruction item : result) {
            if ("LABEL".equals(item.getOp()) && !referenced.contains(item.getResult())) continue;
            withoutOrphans.add(item);
        }
        return withoutOrphans;
    }
}
