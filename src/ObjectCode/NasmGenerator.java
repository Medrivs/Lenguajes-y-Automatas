package ObjectCode;

import IntermediateCode.TacInstruction;
import Lexer.TokenType;
import SymbolTable.Symbol;
import SymbolTable.SymbolTable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// NasmGenerator (generador NASM): transforma el TAC validado en ensamblador Linux x86-64.
// Usa registros de 32 bits para INTEGER/BOOLEAN y SSE2 de 64 bits para REAL.
public class NasmGenerator {
    private final Map<String, TokenType> variableTypes = new LinkedHashMap<>();
    private final Map<String, TokenType> temporaryTypes = new LinkedHashMap<>();
    private final Map<String, String> realConstants = new LinkedHashMap<>();
    private final Set<String> definedLabels = new HashSet<>();
    private final Set<String> usedLabels = new HashSet<>();
    private final StringBuilder body = new StringBuilder();
    private String assembly = "";
    private int realCount;
    private boolean usesRealNegation;

    // generate (generar): valida los cuádruplos y construye el texto NASM completo.
    public String generate(List<TacInstruction> instructions, SymbolTable table) {
        if (instructions == null || table == null) {
            throw new IllegalArgumentException("Error NASM: se requieren el TAC y la tabla de símbolos");
        }
        reset();
        registerVariables(table);
        registerTemporaries(instructions);
        checkLabels(instructions);

        for (TacInstruction item : instructions) {
            emitInstruction(item);
        }
        StringBuilder text = new StringBuilder();
        text.append("; Código NASM Linux x86-64 generado desde TAC optimizado\n");
        text.append("; INTEGER: 32 bits, REAL: double SSE2, BOOLEAN: 0 o 1\n\n");
        if (usesRealNegation || !realConstants.isEmpty()) {
            text.append("section .rodata\n");
            if (usesRealNegation) {
                text.append("asm_real_sign_mask: dq 0x8000000000000000, 0\n");
            }
            for (Map.Entry<String, String> entry : realConstants.entrySet()) {
                text.append(entry.getValue()).append(": dq ").append(entry.getKey()).append("\n");
            }
            text.append("\n");
        }
        text.append("section .bss\n");
        for (Map.Entry<String, TokenType> entry : variableTypes.entrySet()) {
            text.append(variableLabel(entry.getKey())).append(": ")
                    .append(entry.getValue() == TokenType.REAL ? "resq 1" : "resd 1").append("\n");
        }
        for (Map.Entry<String, TokenType> entry : temporaryTypes.entrySet()) {
            text.append(temporaryLabel(entry.getKey())).append(": ")
                    .append(entry.getValue() == TokenType.REAL ? "resq 1" : "resd 1").append("\n");
        }
        text.append("\nsection .text\n");
        text.append("global _start\n_start:\n");
        text.append(body);
        text.append("    mov eax, 60       ; llamada sys_exit de Linux\n");
        text.append("    xor edi, edi      ; código de salida cero\n");
        text.append("    syscall\n");
        assembly = text.toString();
        return assembly;
    }

    // getText (obtener texto): devuelve el último ensamblador generado.
    public String getText() {
        return assembly;
    }

    // writeFile (guardar archivo): exporta el texto ensamblador en UTF-8.
    public void writeFile(Path path) throws IOException {
        Files.writeString(path, assembly, StandardCharsets.UTF_8);
    }

    // reset (reiniciar): permite reutilizar el mismo generador para varios programas.
    private void reset() {
        variableTypes.clear();
        temporaryTypes.clear();
        realConstants.clear();
        definedLabels.clear();
        usedLabels.clear();
        body.setLength(0);
        assembly = "";
        realCount = 0;
        usesRealNegation = false;
    }

    // registerVariables (declarar variables): toma los tipos de la tabla semántica.
    private void registerVariables(SymbolTable table) {
        List<Symbol> symbols = new ArrayList<>(table.getSymbols());
        symbols.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (Symbol symbol : symbols) {
            variableTypes.put(normalize(symbol.getName()), symbol.getType());
        }
    }

    // registerTemporaries (declarar temporales): registra el tipo de cada destino _tN.
    private void registerTemporaries(List<TacInstruction> code) {
        for (TacInstruction item : code) {
            if (item == null || item.getOp() == null) {
                throw new IllegalArgumentException("Error NASM: hay una instrucción TAC vacía");
            }
            String destination = item.getResult();
            if (destination != null && destination.matches("_t[0-9]+")) {
                if (item.getType() == null) {
                    throw new IllegalArgumentException("Error NASM: falta el tipo del temporal " + destination);
                }
                TokenType previous = temporaryTypes.putIfAbsent(destination, item.getType());
                if (previous != null && previous != item.getType()) {
                    throw new IllegalArgumentException("Error NASM: el temporal " + destination + " tiene tipos diferentes");
                }
            }
        }
    }

    // checkLabels (revisar etiquetas): impide saltos a lugares inexistentes o duplicados.
    private void checkLabels(List<TacInstruction> code) {
        for (TacInstruction item : code) {
            if ("LABEL".equals(item.getOp())) {
                checkLabelName(item.getResult());
                if (!definedLabels.add(item.getResult())) {
                    throw new IllegalArgumentException("Error NASM: etiqueta duplicada " + item.getResult());
                }
            } else if ("GOTO".equals(item.getOp()) || "IF_TRUE".equals(item.getOp())) {
                checkLabelName(item.getResult());
                usedLabels.add(item.getResult());
            }
        }
        for (String label : usedLabels) {
            if (!definedLabels.contains(label)) {
                throw new IllegalArgumentException("Error NASM: falta definir la etiqueta " + label);
            }
        }
    }

    // checkLabelName (validar nombre): solo admite etiquetas propias del generador TAC.
    private void checkLabelName(String name) {
        if (name == null || !name.matches("L[0-9]+")) {
            throw new IllegalArgumentException("Error NASM: nombre de etiqueta inválido: " + name);
        }
    }

    // emitInstruction (traducir instrucción): selecciona el ensamblador para cada op.
    private void emitInstruction(TacInstruction item) {
        String op = item.getOp();
        switch (op) {
            case "LABEL":
                body.append(labelName(item.getResult())).append(":\n");
                break;
            case "GOTO":
                emit("jmp " + labelName(item.getResult()));
                break;
            case "IF_TRUE":
                loadInteger("eax", item.getArg1());
                emit("test eax, eax");
                emit("jnz " + labelName(item.getResult()));
                break;
            case "ASSIGN":
                emitAssignment(item);
                break;
            case "I2R":
                requireType(item, TokenType.REAL);
                loadInteger("eax", item.getArg1());
                emit("cvtsi2sd xmm0, eax");
                storeReal(item.getResult(), "xmm0");
                break;
            case "NEG":
                emitNegation(item);
                break;
            case "+": case "-": case "*": case "/":
                emitArithmetic(item);
                break;
            case "==": case "!=": case "<": case "<=": case ">": case ">=":
                emitComparison(item);
                break;
            default:
                throw new IllegalArgumentException("Error NASM: operación TAC desconocida: " + op);
        }
    }

    // emitAssignment (asignar): guarda un entero, booleano o REAL según el destino.
    private void emitAssignment(TacInstruction item) {
        TokenType type = destinationType(item.getResult());
        if (item.getType() != type) {
            throw new IllegalArgumentException("Error NASM: tipo incompatible en la asignación a " + item.getResult());
        }
        if (type == TokenType.REAL) {
            loadReal("xmm0", item.getArg1());
            storeReal(item.getResult(), "xmm0");
        } else {
            loadInteger("eax", item.getArg1());
            storeInteger(item.getResult(), "eax");
        }
    }

    // emitNegation (negar): usa NEG para INTEGER y cambia el signo para REAL.
    private void emitNegation(TacInstruction item) {
        if (item.getType() == TokenType.INTEGER) {
            loadInteger("eax", item.getArg1());
            emit("neg eax");
            storeInteger(item.getResult(), "eax");
        } else if (item.getType() == TokenType.REAL) {
            usesRealNegation = true;
            loadReal("xmm0", item.getArg1());
            emit("xorpd xmm0, [rel asm_real_sign_mask]");
            storeReal(item.getResult(), "xmm0");
        } else {
            throw new IllegalArgumentException("Error NASM: NEG requiere tipo numérico");
        }
    }

    // emitArithmetic (aritmética): opera en eax o xmm0 y almacena el resultado.
    private void emitArithmetic(TacInstruction item) {
        String op = item.getOp();
        if (item.getType() == TokenType.INTEGER) {
            if ("/".equals(op)) {
                throw new IllegalArgumentException("Error NASM: la división TAC debe tener tipo REAL");
            }
            loadInteger("eax", item.getArg1());
            loadInteger("ebx", item.getArg2());
            switch (op) {
                case "+": emit("add eax, ebx"); break;
                case "-": emit("sub eax, ebx"); break;
                default: emit("imul eax, ebx"); break;
            }
            storeInteger(item.getResult(), "eax");
        } else if (item.getType() == TokenType.REAL) {
            loadReal("xmm0", item.getArg1());
            loadReal("xmm1", item.getArg2());
            switch (op) {
                case "+": emit("addsd xmm0, xmm1"); break;
                case "-": emit("subsd xmm0, xmm1"); break;
                case "*": emit("mulsd xmm0, xmm1"); break;
                default: emit("divsd xmm0, xmm1"); break;
            }
            storeReal(item.getResult(), "xmm0");
        } else {
            throw new IllegalArgumentException("Error NASM: operación aritmética sin tipo numérico");
        }
    }

    // emitComparison (comparar): distingue enteros/booleanos de números reales.
    // Las condiciones sobre REAL consideran el caso sin orden (NaN).
    private void emitComparison(TacInstruction item) {
        requireType(item, TokenType.BOOLEAN);
        boolean real = operandType(item.getArg1()) == TokenType.REAL ||
                operandType(item.getArg2()) == TokenType.REAL;
        if (real) {
            loadReal("xmm0", item.getArg1());
            loadReal("xmm1", item.getArg2());
            emit("ucomisd xmm0, xmm1");
            switch (item.getOp()) {
                case "==": emit("sete al"); emit("setnp dl"); emit("and al, dl"); break;
                case "!=": emit("setne al"); emit("setp dl"); emit("or al, dl"); break;
                case "<": emit("setb al"); emit("setnp dl"); emit("and al, dl"); break;
                case "<=": emit("setbe al"); emit("setnp dl"); emit("and al, dl"); break;
                case ">": emit("seta al"); break;
                default: emit("setae al"); break;
            }
        } else {
            loadInteger("eax", item.getArg1());
            loadInteger("ebx", item.getArg2());
            emit("cmp eax, ebx");
            switch (item.getOp()) {
                case "==": emit("sete al"); break;
                case "!=": emit("setne al"); break;
                case "<": emit("setl al"); break;
                case "<=": emit("setle al"); break;
                case ">": emit("setg al"); break;
                default: emit("setge al"); break;
            }
        }
        emit("movzx eax, al");
        storeInteger(item.getResult(), "eax");
    }

    // loadInteger (cargar entero): acepta números, booleanos y símbolos de 32 bits.
    private void loadInteger(String register, String value) {
        if ("TRUE".equals(value) || "FALSE".equals(value)) {
            emit("mov " + register + ", " + ("TRUE".equals(value) ? "1" : "0"));
        } else if (isInteger(value)) {
            BigInteger number = new BigInteger(value);
            if (number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 ||
                    number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("Error NASM: entero fuera del rango de 32 bits: " + value);
            }
            emit("mov " + register + ", " + number.toString());
        } else {
            TokenType type = operandType(value);
            if (type != TokenType.INTEGER && type != TokenType.BOOLEAN) {
                throw new IllegalArgumentException("Error NASM: se esperaba entero o booleano: " + value);
            }
            emit("mov " + register + ", dword [rel " + location(value) + "]");
        }
    }

    // loadReal (cargar REAL): lee valores de 64 bits y convierte enteros si llegan.
    private void loadReal(String register, String value) {
        TokenType type = operandType(value);
        if (type == TokenType.INTEGER) {
            loadInteger("eax", value);
            emit("cvtsi2sd " + register + ", eax");
        } else if (type == TokenType.REAL) {
            String source = isReal(value) ? constantLabel(value) : location(value);
            emit("movsd " + register + ", qword [rel " + source + "]");
        } else {
            throw new IllegalArgumentException("Error NASM: se esperaba un valor REAL: " + value);
        }
    }

    // storeInteger (guardar entero): escribe los 32 bits bajos del registro.
    private void storeInteger(String destination, String register) {
        TokenType type = destinationType(destination);
        if (type != TokenType.INTEGER && type != TokenType.BOOLEAN) {
            throw new IllegalArgumentException("Error NASM: destino de 32 bits inválido: " + destination);
        }
        emit("mov dword [rel " + location(destination) + "], " + register);
    }

    // storeReal (guardar REAL): escribe el double de 64 bits en memoria.
    private void storeReal(String destination, String register) {
        if (destinationType(destination) != TokenType.REAL) {
            throw new IllegalArgumentException("Error NASM: destino REAL inválido: " + destination);
        }
        emit("movsd qword [rel " + location(destination) + "], " + register);
    }

    // operandType (consultar tipo): distingue literales y símbolos declarados.
    private TokenType operandType(String value) {
        if ("TRUE".equals(value) || "FALSE".equals(value)) return TokenType.BOOLEAN;
        if (isInteger(value)) return TokenType.INTEGER;
        if (isReal(value)) return TokenType.REAL;
        if (value != null && temporaryTypes.containsKey(value)) return temporaryTypes.get(value);
        TokenType type = variableTypes.get(normalize(value));
        if (type == null) {
            throw new IllegalArgumentException("Error NASM: operando sin declarar: " + value);
        }
        return type;
    }

    // destinationType (tipo del destino): localiza variables y temporales.
    private TokenType destinationType(String destination) {
        if (destination != null && temporaryTypes.containsKey(destination)) return temporaryTypes.get(destination);
        TokenType type = variableTypes.get(normalize(destination));
        if (type == null) {
            throw new IllegalArgumentException("Error NASM: destino sin declarar: " + destination);
        }
        return type;
    }

    // location (dirección): evita confundir símbolos Pascal con registros NASM.
    private String location(String name) {
        if (name != null && temporaryTypes.containsKey(name)) return temporaryLabel(name);
        if (variableTypes.containsKey(normalize(name))) return variableLabel(normalize(name));
        throw new IllegalArgumentException("Error NASM: símbolo desconocido: " + name);
    }

    // constantLabel (real constante): registra un literal para section .rodata.
    private String constantLabel(String value) {
        String label = realConstants.get(value);
        if (label == null) {
            realCount++;
            label = "asm_real_const_" + realCount;
            realConstants.put(value, label);
        }
        return label;
    }

    // normalize (normalizar): compara los identificadores Pascal sin importar caso.
    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    // isInteger (es entero): detecta un literal con signo opcional.
    private boolean isInteger(String value) {
        return value != null && value.matches("-?[0-9]+");
    }

    // isReal (es real): reconoce números decimales finitos del optimizador TAC.
    private boolean isReal(String value) {
        if (value == null || !value.matches("-?(?:[0-9]+\\.[0-9]*|[0-9]+(?:\\.[0-9]*)?[Ee][+-]?[0-9]+)")) return false;
        try {
            return Double.isFinite(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    // labelName (etiqueta NASM): separa las etiquetas del espacio de variables.
    private String labelName(String label) {
        checkLabelName(label);
        return "asm_" + label;
    }

    // requireType (comprobar tipo): evita emitir operaciones con un tipo incorrecto.
    private void requireType(TacInstruction item, TokenType expected) {
        if (item.getType() != expected) {
            throw new IllegalArgumentException("Error NASM: tipo TAC incorrecto en " + item.getOp());
        }
    }

    // emit (escribir): agrega una instrucción ensamblador con sangría.
    private void emit(String line) {
        body.append("    ").append(line).append("\n");
    }

    // variableLabel (etiqueta variable): asigna un prefijo único y estable.
    private String variableLabel(String name) {
        return "asm_v_" + normalize(name);
    }

    // temporaryLabel (etiqueta temporal): reserva un espacio distinto al usuario.
    private String temporaryLabel(String name) {
        return "asm_tmp_" + name.substring(2);
    }
}
