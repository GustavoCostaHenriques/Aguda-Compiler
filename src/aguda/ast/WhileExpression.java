package aguda.ast;

public class WhileExpression extends AbstractAstNode {
    public final AstNode condition;
    public final AstNode body;

    public WhileExpression(int line, int column, AstNode condition, AstNode body) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + "while " + condition.print(indent) + " do\n" + " ".repeat(indent + 2) + body.print(indent + 2);
    }
}