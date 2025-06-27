package aguda.ast;

import java.util.List;

public class Program extends AbstractAstNode {
    public final List<AstNode> declarations;

    public Program(int line, int column, List<AstNode> declarations) {
        super(line, column);
        this.declarations = declarations;
    }

    @Override
    public String print(int indent) {
        StringBuilder sb = new StringBuilder();
        for (AstNode decl : declarations) {
            sb.append(decl.print(indent)).append("\n");
        }

        
        String[] lines = sb.toString().trim().split("\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String cleanedLine = line.replaceAll("(?<=\\S) {2,}", " ");

            cleanedLine = cleanedLine.replaceAll("(?<![ \\(\\[])\\)", " )");
            cleanedLine = cleanedLine.replaceAll("(?<![ \\[\\[])\\]", " ]");

            cleaned.append(cleanedLine).append("\n");
        }

        return cleaned.toString().trim(); 
    }
}
