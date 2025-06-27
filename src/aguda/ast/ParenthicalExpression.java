package aguda.ast;

public class ParenthicalExpression extends AbstractAstNode {
    public final AstNode expression;

    public ParenthicalExpression(int line, int column, AstNode expression) {
        super(line, column);
        this.expression = expression;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + "(\n" + expression.print(indent + 2) + "\n" + " ".repeat(indent) + ")";
    }
}