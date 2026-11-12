package TestMain;

import Lexer.Lexer;
import Lexer.Token;
import Lexer.TokenType;
import Parser.Parser;
import Parser.ProgramNode;
import SymbolTable.Symbol;
import SymbolTable.SymbolTable;
import Semantic.SemanticAnalyzer;
import Visitor.AstPrinterVisitor;
import IntermediateCode.TacGenerator;
import IntermediateCode.TacInstruction;
import IntermediateCode.TacOptimizer;
import ObjectCode.NasmGenerator;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

// Main (inicio de las pruebas): muestra los tokens y comprueba el árbol sintáctico.
public class Main {
    private final String sourceCode;

    // Main (constructor): define un programa de ejemplo del lenguaje Pascal Subset.
    public Main() {
        sourceCode = "PROGRAM Demo;\n" +
                "VAR\n" +
                "    counter: INTEGER;\n" +
                "    ready: BOOLEAN;\n" +
                "BEGIN\n" +
                "    counter := 2 * 3 - 6;\n" +
                "    ready := TRUE;\n" +
                "    WHILE (counter < 3) AND ready DO\n" +
                "    BEGIN\n" +
                "        counter := counter + 1;\n" +
                "    END;\n" +
                "END.";
    }

    // main (método principal): inicia la prueba del compilador.
    public static void main(String[] args) {
        Main program = new Main();
        program.run();
    }

    // run (ejecutar prueba): imprime tokens y revisa la estructura del AST.
    // Aquí NO se ejecutan las sentencias del programa Pascal.
    private void run() {
        System.out.println("TOKENS DEL ANALIZADOR LÉXICO");
        Lexer testLexer = new Lexer(sourceCode);
        Token token = testLexer.nextToken();

        while (token.getType() != TokenType.EOF) {
            System.out.println(token);
            token = testLexer.nextToken();
        }

        System.out.println("\nANÁLISIS SINTÁCTICO Y AST");
        Parser parser = new Parser(new Lexer(sourceCode));
        ProgramNode tree = parser.parseProgram();

        System.out.println("Programa: " + tree.getName());
        System.out.println("Declaraciones: " + tree.getDeclarations().size());
        System.out.println("Sentencias del bloque principal: " + tree.getBody().getStatements().size());
        System.out.println("Sintaxis válida. AST construido correctamente.");

        // Crea la tabla de símbolos con las declaraciones del AST.
        System.out.println("\nTABLA DE SÍMBOLOS");
        SymbolTable table = new SymbolTable(tree);
        for (Symbol symbol : table.getSymbols()) {
            System.out.println(symbol);
        }
        System.out.println("Variables registradas: " + table.size());

        // Revisa que los tipos y usos de las variables sean correctos.
        System.out.println("\nANÁLISIS SEMÁNTICO");
        SemanticAnalyzer analyzer = new SemanticAnalyzer(table);
        analyzer.analyze(tree);
        System.out.println("Análisis semántico finalizado correctamente.");

        // Imprime los tipos que el semántico guardó en los nodos del AST.
        System.out.println("\nAST DECORADO (VISITOR)");
        AstPrinterVisitor printer = new AstPrinterVisitor();
        tree.accept(printer);
        System.out.print(printer.getText());

        // Genera instrucciones TAC a partir del AST ya validado y decorado.
        System.out.println("\nCÓDIGO INTERMEDIO TAC");
        TacGenerator generator = new TacGenerator();
        generator.generate(tree);
        System.out.print(generator.getText());

        // Optimiza el TAC sin cambiar el programa ni sus instrucciones originales.
        System.out.println("\nCÓDIGO INTERMEDIO TAC OPTIMIZADO");
        TacOptimizer optimizer = new TacOptimizer();
        List<TacInstruction> optimized = optimizer.optimize(generator.getCode());
        for (TacInstruction instruction : optimized) {
            System.out.println(instruction);
        }

        // Traduce el TAC optimizado a ensamblador NASM para Linux x86-64.
        System.out.println("\nCÓDIGO OBJETO NASM x86-64");
        NasmGenerator nasm = new NasmGenerator();
        nasm.generate(optimized, table);
        System.out.print(nasm.getText());

        // Guarda un .asm listo para NASM dentro de la carpeta generada build.
        // No utiliza la terminal ni modifica los archivos fuente del proyecto.
        Path output = Path.of("build", "programa.asm");
        try {
            Files.createDirectories(output.getParent());
            nasm.writeFile(output);
            System.out.println("Archivo NASM guardado en: " + output.toAbsolutePath());
        } catch (IOException exception) {
            System.out.println("No se pudo guardar el archivo NASM: " + exception.getMessage());
        }
    }
}
