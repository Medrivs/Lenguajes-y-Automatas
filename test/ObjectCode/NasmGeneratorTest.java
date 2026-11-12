package ObjectCode;

import IntermediateCode.TacGenerator;
import IntermediateCode.TacInstruction;
import IntermediateCode.TacOptimizer;
import Lexer.Lexer;
import Lexer.TokenType;
import Parser.Parser;
import Parser.ProgramNode;
import Semantic.SemanticAnalyzer;
import SymbolTable.SymbolTable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// NasmGeneratorTest (pruebas NASM): verifica instrucciones y su integración con TAC.
// Las pruebas no requieren tener instalado NASM para revisar el texto generado.
public class NasmGeneratorTest {
    private static int passed;

    // check (comprobar): cuenta una condición correcta o explica el fallo.
    private static void check(boolean condition, String message) {
        if (!condition) throw new RuntimeException("Falló una prueba NASM: " + message);
        passed++;
    }

    // has (contiene): exige una instrucción o sección en el resultado.
    private static void has(String code, String fragment) {
        check(code.contains(fragment), "se esperaba: " + fragment);
    }

    // lacks (no contiene): evita instrucciones que no deberían generarse.
    private static void lacks(String code, String fragment) {
        check(!code.contains(fragment), "no se esperaba: " + fragment);
    }

    // program (programa): crea código Pascal Subset válido para las pruebas.
    private static String program(String declarations, String statements) {
        return "PROGRAM Demo;\n" + declarations + "\nBEGIN\n" + statements + "\nEND.";
    }

    // parse (analizar): construye el AST para obtener declaraciones y tipos.
    private static ProgramNode parse(String source) {
        return new Parser(new Lexer(source)).parseProgram();
    }

    // generate (generar): ejecuta las fases anteriores antes de generar NASM.
    private static String generate(String source, boolean optimized) {
        ProgramNode tree = parse(source);
        SymbolTable table = new SymbolTable(tree);
        new SemanticAnalyzer(table).analyze(tree);
        List<TacInstruction> instructions = new TacGenerator().generate(tree);
        if (optimized) instructions = new TacOptimizer().optimize(instructions);
        return new NasmGenerator().generate(instructions, table);
    }

    // raw (generar TAC manual): permite comprobar cada operador por separado.
    private static String raw(String declarations, TacInstruction... instructions) {
        SymbolTable table = new SymbolTable(parse(program(declarations, "")));
        return new NasmGenerator().generate(Arrays.asList(instructions), table);
    }

    // tac (cuádruplo): crea una instrucción TAC para los ejemplos aislados.
    private static TacInstruction tac(String op, String a, String b, String dest, TokenType type) {
        return new TacInstruction(op, a, b, dest, type);
    }

    // invalid (debe fallar): verifica mensajes de error en español.
    private static void invalid(String declarations, String expected, TacInstruction... instructions) {
        try {
            raw(declarations, instructions);
        } catch (IllegalArgumentException ex) {
            check(ex.getMessage().contains(expected), "mensaje incorrecto: " + ex.getMessage());
            return;
        }
        check(false, "se esperaba un error: " + expected);
    }

    // checkSections (secciones): prueba la estructura mínima de Linux x86-64.
    private static void checkSections() {
        String asm = generate(program("", ""), true);
        has(asm, "section .bss");
        has(asm, "section .text");
        has(asm, "global _start");
        has(asm, "_start:");
        has(asm, "mov eax, 60");
        has(asm, "xor edi, edi");
        has(asm, "syscall");
        lacks(asm, "extern printf");
        lacks(asm, "section .rodata");
        check(asm.indexOf("_start:") < asm.indexOf("syscall"), "salida después del inicio");
    }

    // checkInteger (enteros): comprueba memoria de 32 bits y aritmética.
    private static void checkInteger() {
        String asm = raw("VAR a,b,c: INTEGER;",
                tac("ASSIGN", "2", null, "a", TokenType.INTEGER),
                tac("ASSIGN", "3", null, "b", TokenType.INTEGER),
                tac("+", "a", "b", "_t1", TokenType.INTEGER),
                tac("-", "a", "b", "_t2", TokenType.INTEGER),
                tac("*", "a", "b", "_t3", TokenType.INTEGER),
                tac("ASSIGN", "_t1", null, "c", TokenType.INTEGER));
        has(asm, "asm_v_A: resd 1");
        has(asm, "asm_v_B: resd 1");
        has(asm, "asm_v_C: resd 1");
        has(asm, "asm_tmp_1: resd 1");
        has(asm, "asm_tmp_2: resd 1");
        has(asm, "asm_tmp_3: resd 1");
        has(asm, "add eax, ebx");
        has(asm, "sub eax, ebx");
        has(asm, "imul eax, ebx");
        has(asm, "mov eax, dword [rel asm_v_A]");
        has(asm, "mov ebx, dword [rel asm_v_B]");
        has(asm, "mov dword [rel asm_tmp_1], eax");
        has(asm, "mov dword [rel asm_v_C], eax");
        lacks(asm, "asm_v_a:");
    }

    // checkReal (reales): comprueba SSE2 y los literales de doble precisión.
    private static void checkReal() {
        String asm = raw("VAR a,b,c: REAL;",
                tac("ASSIGN", "2.5", null, "a", TokenType.REAL),
                tac("ASSIGN", "1.25", null, "b", TokenType.REAL),
                tac("+", "a", "b", "_t1", TokenType.REAL),
                tac("-", "a", "b", "_t2", TokenType.REAL),
                tac("*", "a", "b", "_t3", TokenType.REAL),
                tac("/", "a", "b", "_t4", TokenType.REAL),
                tac("ASSIGN", "_t4", null, "c", TokenType.REAL));
        has(asm, "section .rodata");
        has(asm, "asm_real_const_1: dq 2.5");
        has(asm, "asm_real_const_2: dq 1.25");
        has(asm, "asm_v_A: resq 1");
        has(asm, "asm_tmp_4: resq 1");
        has(asm, "movsd xmm0, qword [rel asm_real_const_1]");
        has(asm, "movsd xmm1, qword [rel asm_v_B]");
        has(asm, "addsd xmm0, xmm1");
        has(asm, "subsd xmm0, xmm1");
        has(asm, "mulsd xmm0, xmm1");
        has(asm, "divsd xmm0, xmm1");
        has(asm, "movsd qword [rel asm_v_C], xmm0");
        check(asm.indexOf("asm_real_const_1: dq 2.5") == asm.lastIndexOf("asm_real_const_1: dq 2.5"), "constante sin duplicados");
    }

    // checkConversions (conversiones): verifica I2R y división que devuelve REAL.
    private static void checkConversions() {
        String asm = generate(program("VAR x: INTEGER; r: REAL;", "x := 9; r := x / 2;"), false);
        has(asm, "cvtsi2sd xmm0, eax");
        has(asm, "divsd xmm0, xmm1");
        has(asm, "asm_v_X: resd 1");
        has(asm, "asm_v_R: resq 1");
        lacks(asm, "idiv");
        String simple = raw("VAR i: INTEGER; r: REAL;",
                tac("ASSIGN", "5", null, "i", TokenType.INTEGER),
                tac("I2R", "i", null, "_t1", TokenType.REAL),
                tac("ASSIGN", "_t1", null, "r", TokenType.REAL));
        has(simple, "cvtsi2sd xmm0, eax");
        has(simple, "asm_tmp_1: resq 1");
        String folded = generate(program("VAR r: REAL;", "r := 9 / 2;"), true);
        has(folded, "asm_v_R: resq 1");
        has(folded, "asm_real_const_1:");
    }

    // checkNegation (signos): comprueba signo entero y signo bit a bit en double.
    private static void checkNegation() {
        String asm = raw("VAR i: INTEGER; r: REAL;",
                tac("NEG", "5", null, "_t1", TokenType.INTEGER),
                tac("ASSIGN", "_t1", null, "i", TokenType.INTEGER),
                tac("NEG", "2.5", null, "_t2", TokenType.REAL),
                tac("ASSIGN", "_t2", null, "r", TokenType.REAL));
        has(asm, "neg eax");
        has(asm, "asm_real_sign_mask: dq 0x8000000000000000, 0");
        has(asm, "xorpd xmm0, [rel asm_real_sign_mask]");
        has(asm, "asm_tmp_2: resq 1");
        lacks(asm, "fchs");
    }

    // checkComparisons (comparaciones): revisa los saltos y las banderas de CPU.
    private static void checkComparisons() {
        String asm = raw("VAR x,y: INTEGER; b: BOOLEAN;",
                tac("<", "x", "y", "_t1", TokenType.BOOLEAN),
                tac("<=", "x", "y", "_t2", TokenType.BOOLEAN),
                tac(">", "x", "y", "_t3", TokenType.BOOLEAN),
                tac(">=", "x", "y", "_t4", TokenType.BOOLEAN),
                tac("==", "x", "y", "_t5", TokenType.BOOLEAN),
                tac("!=", "x", "y", "_t6", TokenType.BOOLEAN),
                tac("ASSIGN", "_t1", null, "b", TokenType.BOOLEAN));
        has(asm, "cmp eax, ebx");
        has(asm, "setl al");
        has(asm, "setle al");
        has(asm, "setg al");
        has(asm, "setge al");
        has(asm, "sete al");
        has(asm, "setne al");
        has(asm, "movzx eax, al");
        has(asm, "asm_v_B: resd 1");
        String floating = raw("VAR r,s: REAL; b: BOOLEAN;",
                tac("<", "r", "s", "_t1", TokenType.BOOLEAN),
                tac("<=", "r", "s", "_t2", TokenType.BOOLEAN),
                tac(">", "r", "s", "_t3", TokenType.BOOLEAN),
                tac(">=", "r", "s", "_t4", TokenType.BOOLEAN),
                tac("==", "r", "s", "_t5", TokenType.BOOLEAN),
                tac("!=", "r", "s", "_t6", TokenType.BOOLEAN),
                tac("ASSIGN", "_t1", null, "b", TokenType.BOOLEAN));
        has(floating, "ucomisd xmm0, xmm1");
        has(floating, "setb al");
        has(floating, "setbe al");
        has(floating, "seta al");
        has(floating, "setae al");
        has(floating, "setnp dl");
        has(floating, "setp dl");
        has(floating, "and al, dl");
        has(floating, "or al, dl");
        lacks(floating, "cmp eax, ebx");
    }

    // checkControlFlow (flujo): traduce los saltos del Visitor y el optimizador.
    private static void checkControlFlow() {
        String asm = generate(program("VAR x: INTEGER; b: BOOLEAN;",
                "x := 0; b := TRUE; WHILE x < 3 DO BEGIN IF b THEN x := x + 1 ELSE x := x + 2; END;"), false);
        has(asm, "asm_L1:");
        has(asm, "jnz asm_L");
        has(asm, "jmp asm_L");
        has(asm, "test eax, eax");
        has(asm, "asm_v_B: resd 1");
        String opt = generate(program("VAR x: INTEGER; b: BOOLEAN;",
                "x := 0; b := TRUE; WHILE x < 3 DO BEGIN IF b THEN x := x + 1 ELSE x := x + 2; END;"), true);
        has(opt, "asm_L");
        has(opt, "jmp asm_L");
        has(opt, "section .text");
        lacks(opt, "if ");
        String booleans = generate(program("VAR a,b: BOOLEAN; x: INTEGER;",
                "a := FALSE; b := TRUE; x := 0; IF a OR b THEN x := 2 ELSE x := 3;"), false);
        has(booleans, "asm_L");
        has(booleans, "jnz asm_L");
        lacks(booleans, "AND");
        lacks(booleans, "OR\n");
    }

    // checkBoolean (booleanos): confirma valores TRUE=1 y FALSE=0.
    private static void checkBoolean() {
        String asm = raw("VAR b: BOOLEAN;",
                tac("ASSIGN", "TRUE", null, "b", TokenType.BOOLEAN),
                tac("ASSIGN", "FALSE", null, "_t1", TokenType.BOOLEAN),
                tac("IF_TRUE", "b", null, "L1", null),
                tac("ASSIGN", "_t1", null, "b", TokenType.BOOLEAN),
                tac("LABEL", null, null, "L1", null));
        has(asm, "mov eax, 1");
        has(asm, "mov eax, 0");
        has(asm, "asm_tmp_1: resd 1");
        has(asm, "test eax, eax");
        has(asm, "jnz asm_L1");
        has(asm, "asm_L1:");
    }

    // checkNames (nombres): valida normalización y estabilidad de la salida.
    private static void checkNames() {
        String asm = generate(program("VAR Counter: INTEGER; pi: REAL; ready: BOOLEAN;",
                "COUNTER := 2; Pi := 3.14; READY := counter < 5;"), false);
        has(asm, "asm_v_COUNTER: resd 1");
        has(asm, "asm_v_PI: resq 1");
        has(asm, "asm_v_READY: resd 1");
        lacks(asm, "asm_v_counter:");
        NasmGenerator generator = new NasmGenerator();
        ProgramNode tree = parse(program("VAR x: INTEGER;", "x := 1;"));
        SymbolTable symbols = new SymbolTable(tree);
        new SemanticAnalyzer(symbols).analyze(tree);
        List<TacInstruction> code = new TacGenerator().generate(tree);
        String first = generator.generate(code, symbols);
        String second = generator.generate(code, symbols);
        check(first.equals(second), "salida reproducible al reutilizar el generador");
        lacks(second, "asm_real_const_1");
        check(generator.getText().equals(second), "getText devuelve el ensamblador actual");
    }

    // checkExport (guardar): escribe un archivo .asm de ejemplo en UTF-8.
    private static void checkExport() throws Exception {
        ProgramNode tree = parse(program("VAR x: INTEGER;", "x := 9;"));
        SymbolTable symbols = new SymbolTable(tree);
        new SemanticAnalyzer(symbols).analyze(tree);
        NasmGenerator generator = new NasmGenerator();
        generator.generate(new TacOptimizer().optimize(new TacGenerator().generate(tree)), symbols);
        Path file = Files.createTempFile("nasm-pascal-test-", ".asm");
        try {
            generator.writeFile(file);
            check(Files.readString(file, StandardCharsets.UTF_8).equals(generator.getText()), "archivo asm idéntico al texto");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    // checkErrors (errores): evita emitir ensamblador ante TAC inconsistente.
    private static void checkErrors() {
        invalid("VAR a: INTEGER;", "falta definir la etiqueta", tac("GOTO", null, null, "L999", null));
        invalid("", "etiqueta duplicada", tac("LABEL", null, null, "L1", null), tac("LABEL", null, null, "L1", null));
        invalid("", "nombre de etiqueta inválido", tac("GOTO", null, null, "LOOP", null));
        invalid("", "operación TAC desconocida", tac("PRINT", "x", null, "_t1", TokenType.INTEGER));
        invalid("", "destino sin declarar", tac("ASSIGN", "1", null, "x", TokenType.INTEGER));
        invalid("VAR a: INTEGER;", "tipo incompatible", tac("ASSIGN", "1", null, "a", TokenType.REAL));
        invalid("", "falta el tipo", tac("ASSIGN", "1", null, "_t1", null));
        invalid("", "tipos diferentes", tac("ASSIGN", "1", null, "_t1", TokenType.INTEGER),
                tac("ASSIGN", "TRUE", null, "_t1", TokenType.BOOLEAN));
        invalid("VAR a: INTEGER;", "operando sin declarar", tac("ASSIGN", "x", null, "a", TokenType.INTEGER));
        invalid("VAR a: INTEGER;", "entero fuera del rango", tac("ASSIGN", "2147483648", null, "a", TokenType.INTEGER));
        invalid("VAR a: INTEGER;", "división TAC debe tener tipo REAL", tac("/", "a", "2", "_t1", TokenType.INTEGER));
        invalid("VAR a: BOOLEAN;", "NEG requiere tipo numérico", tac("NEG", "a", null, "_t1", TokenType.BOOLEAN));
        invalid("VAR a: BOOLEAN;", "tipo TAC incorrecto", tac("<", "1", "2", "a", TokenType.INTEGER));
        invalid("VAR a: REAL;", "se esperaba entero o booleano", tac("I2R", "a", null, "_t1", TokenType.REAL));
    }

    // main (ejecutar pruebas): revisa ensamblador e integración sin JUnit.
    public static void main(String[] args) throws Exception {
        checkSections();
        checkInteger();
        checkReal();
        checkConversions();
        checkNegation();
        checkComparisons();
        checkControlFlow();
        checkBoolean();
        checkNames();
        checkExport();
        checkErrors();
        System.out.println("Pruebas del generador NASM superadas: " + passed);
    }
}
