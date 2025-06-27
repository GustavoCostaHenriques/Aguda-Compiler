package app;

import aguda.parser.*;
import aguda.ast.*;
import aguda.checker.*;
import aguda.codegen.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java Main <file.agu> [--ast | --parser | --test-syntax <max_number_of_errors> | --test-semantic <max_number_of_errors> | --test-execution <max_number_of_errors>]");
            System.exit(1);
        }

        String filename = args[0];
        String mode = args[1];
        
        if (!mode.equals("--ast") && !mode.equals("--parser") && !mode.equals("--test-syntax") && !mode.equals("--test-semantic") && !mode.equals("--test-execution")) {
            System.err.println("Unknown flag: " + mode);
            System.err.println("Use one of: --ast, --parser, --test-syntax <max_number_of_errors>, --test-semantic <max_number_of_errors>, --test-execution <max_number_of_errors>");
            System.exit(1);
        }
        int maxErrors = args.length > 2 ? Integer.parseInt(args[2]) : 10;

        CharStream input = CharStreams.fromFileName(filename);
        AgudaLexer lexer = new AgudaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AgudaParser parser = new AgudaParser(tokens);
        Checker checker = null;

        class ErrorInfo {
            String type;
            int line;
            int column;
            String message;

            ErrorInfo(String type, int line, int column, String message) {
                this.type = type;
                this.line = line;
                this.column = column;
                this.message = message;
            }
        }

        List<ErrorInfo> errors = new ArrayList<>();

        if (mode.equals("--test-syntax") || mode.equals("--test-semantic") || mode.equals("--test-execution")) {
            lexer.removeErrorListeners();
            parser.removeErrorListeners();

            lexer.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine, String msg, RecognitionException e) {
                    if (errors.size() < maxErrors) {
                        errors.add(new ErrorInfo("Lexical error", line, charPositionInLine, msg));
                    }
                }
            });

            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine, String msg, RecognitionException e) {
                    if (errors.size() < maxErrors) {
                        errors.add(new ErrorInfo("Syntactic error", line, charPositionInLine, msg));
                    }
                }
            });
        }

        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.setBuildParseTree(true);
        ParseTree tree = parser.program();

        // HANDLE TEST MODE BEFORE DOING ANYTHING ELSE
        if (mode.equals("--test-syntax") || mode.equals("--test-semantic") || mode.equals("--test-execution")) {
            if (!errors.isEmpty()) {
                System.out.println("Test failed ❌");
                try {
                    List<String> lines = Files.readAllLines(Paths.get(filename));
                    for (ErrorInfo err : errors) {
                        System.out.printf("\n%s: line %d, column %d\n", err.type, err.line, err.column);
                        System.out.println("Description: " + err.message);
                        if (err.line > 0 && err.line <= lines.size()) {
                            String codeLine = lines.get(err.line - 1);
                            System.out.printf(">> %s\n", codeLine);
                            System.out.println(" ".repeat(err.column) + "   " + "^");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Could not read source file for error display.");
                }
                System.exit(1);
            } else {
                if(mode.equals("--test-syntax")) {
                    System.out.println("Test passed ✅");
                    System.exit(0);
                } 
            }
        }

        // ONLY DO AST/PARSER MODE IF NOT TEST-SYNTAX MODE
        if (tree == null || parser.getNumberOfSyntaxErrors() > 0) {
            System.out.println("Test failed ❌");
            System.out.println("Parsing failed: could not build parse tree.");

            if (!errors.isEmpty()) {
                List<String> lines = Files.readAllLines(Paths.get(filename));
                for (ErrorInfo err : errors) {
                    System.out.printf("\n%s: line %d, column %d\n", err.type, err.line, err.column);
                    System.out.println("Description: " + err.message);
                    if (err.line > 0 && err.line <= lines.size()) {
                        String codeLine = lines.get(err.line - 1);
                        System.out.printf(">> %s\n", codeLine);
                        System.out.println(" ".repeat(err.column) + "   " + "^");
                    }
                }
            }
            System.exit(1);
        }

        if (mode.equals("--parser")) {
            System.out.println("Parser Tree:");
            try {
                System.out.println(tree.toStringTree(parser));
            } catch (Exception e) {
                System.err.println("⚠️  Failed to print parser tree due to an internal error.");
                System.err.println("Reason: " + e.getMessage());
            }
        }

        AstBuilder builder = new AstBuilder();
        AstNode ast = builder.visit(tree);

        switch (mode) {
            case "--ast" -> {
                System.out.println("AST:");
                System.out.println(ast.print(0));
            }
            case "--test-semantic" -> {
                checker = new Checker(filename, maxErrors);
                try {
                    checker.typeof(ast, true);
                    System.out.println("Test Valid ✅");
                } catch (RuntimeException e) {
                    checker.registerInternalError(e.getMessage());
                    System.out.println(checker.getErrorMessage());
                    System.exit(1);
                }
            }
            case "--test-execution" -> {
                checker = new Checker(filename, maxErrors);
                try {
                    checker.typeof(ast, true);
                } catch(RuntimeException e) {
                    checker.registerInternalError(e.getMessage());
                    System.out.println(checker.getErrorMessage());
                    System.exit(1);
                }
                if (checker != null && !checker.hasErrors()) {
                    CodeGenerator generator = new CodeGenerator(maxErrors);
                    String llvmCode = generator.generate(ast);

                    String outputPath = filename.replace(".agu", "") + ".ll";


                    if (!generator.getErrorMessage().equals("")) {
                        String baseFilename = outputPath.replaceAll("\\.ll$", "");
                        String outputerror = baseFilename + ".err";
                        System.out.println(generator.getErrorMessage());
                        try (FileWriter writer = new FileWriter(outputerror)){
                            writer.write(generator.getErrorMessage());
                        } catch (IOException e) {
                            System.err.println("Error writing Output error file: " + e.getMessage());
                            System.exit(1);
                        }
                        System.exit(1);
                    }

                    // Write the test itself (copy .agu file content to the logs/codegen folder)
                    String testToexecute = outputPath.replace(".ll", ".agu");
                    try {
                        String testContent = Files.readString(Paths.get(filename));

                        try (FileWriter writer = new FileWriter(testToexecute)) {
                            writer.write(testContent);
                        }
                    } catch (IOException e) {
                        System.err.println("Error writing test file: " + e.getMessage());
                        System.exit(1);
                    }

                    // Write the expect file (copy .exp file content to the logs/codegen folder)
                    String expectTargetPath = filename.replace(".agu", ".expect");
                    String outputPathExpect = outputPath.replace(".ll", ".expect");
                    String expContent = null;
                    try {
                        expContent = Files.readString(Paths.get(expectTargetPath));

                        try (FileWriter writer = new FileWriter(outputPathExpect)) {
                            writer.write(expContent);
                        }
                    } catch (IOException e) {
                        System.err.println("Error writing expected output file: " + e.getMessage());
                        System.exit(1); 
                    }

                    // 1. Write llvmCode to a .ll file
                    try (FileWriter writer = new FileWriter(outputPath)) {
                        writer.write(llvmCode);
                    } catch (IOException e) {
                        System.err.println("Error writing LLVM file: " + e.getMessage());
                        System.exit(1);
                    }

                    try {
                        String baseFilename = outputPath.replaceAll("\\.ll$", "");

                        // 2. Generate .s com llc
                        ProcessBuilder pb1 = new ProcessBuilder("llc", outputPath, "-o", baseFilename + ".s");
                        pb1.inheritIO();
                        Process p1 = pb1.start();
                        p1.waitFor();

                        // 3. Compile to binary with clang
                        ProcessBuilder pb2 = new ProcessBuilder("clang", baseFilename + ".s", "-o", baseFilename + ".out", "-no-pie");
                        pb2.inheritIO();
                        Process p2 = pb2.start();
                        p2.waitFor();

                        // 4. Make the binary executable
                        ProcessBuilder chmod = new ProcessBuilder("chmod", "+x", baseFilename + ".out");
                        chmod.inheritIO();
                        Process pchmod = chmod.start();
                        pchmod.waitFor();

                        // 5. Execute the binary and redirect output
                        StringBuilder outputText = new StringBuilder();

                        ProcessBuilder pb3 = new ProcessBuilder(baseFilename + ".out");
                        pb3.redirectError(ProcessBuilder.Redirect.INHERIT);
                        Process p3 = pb3.start();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p3.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                outputText.append(line).append("\n");
                            }
                        }
                        p3.waitFor();
                        try (FileWriter writer = new FileWriter(baseFilename + ".txt")) {
                            writer.write(outputText.toString());
                        }
                        String programOutput = outputText.toString();

                        // 6. Clean up .s and .out
                        new File(baseFilename + ".s").delete();
                        new File(baseFilename + ".out").delete();

                        if (programOutput.trim().equals(expContent.trim())) {
                            System.out.println("Test Valid ✅");
                            System.exit(0);
                        } else {
                            System.out.println("Test failed ❌");
                            System.out.println(String.format("Value different from the expected output. Got %s and the expected was %s", programOutput, expContent));
                            System.exit(1);
                        }

                        System.out.println("Test Valid ✅");
                        System.exit(0);

                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error during assembly generation or execution: " + e.getMessage());
                        System.exit(1);
                    }

                } else {
                    System.exit(1);
                }
            }
            default -> {
                System.err.println("Unknown flag: " + mode);
                System.err.println("Use one of: --ast, --parser, --test-syntax <max_number_of_errors>, --test-semantic <max_number_of_errors>, --test-execution <max_number_of_errors>");
                System.exit(1);
            }
        }
    }
}