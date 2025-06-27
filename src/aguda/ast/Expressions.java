package aguda.ast;

import java.util.List;

public class Expressions extends AbstractAstNode {
    public final List<AstNode> expressions;
    private final String separator;

    public Expressions(int line, int column, List<AstNode> expressions, String separator) {
        super(line, column);
        this.expressions = expressions;
        this.separator = separator;
    }

    @Override
    public String print(int indent) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < expressions.size(); i++) {
            sb.append(expressions.get(i).print(indent));
            if (i < expressions.size() - 1) {
                sb.append(separator);
                if(separator.equals(",")) {
                    sb.append(" ");
                } else {    
                    sb.append("\n");
                }
            }

        }

        return sb.toString();
    }
}
