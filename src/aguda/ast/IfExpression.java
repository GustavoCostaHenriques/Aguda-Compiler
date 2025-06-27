package aguda.ast;

public class IfExpression extends AbstractAstNode {
    public final AstNode condition;
    public final AstNode thenBranch;
    public final AstNode elseBranch;

    public IfExpression(int line, int column, AstNode condition, AstNode thenBranch, AstNode elseBranch) {
        super(line, column);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public String print(int indent) {
        if(elseBranch == null) {
            return " ".repeat(indent) + "if " + condition.print(indent) + " then " + thenBranch.print(indent) + "\n" +" ".repeat(indent) + "else Unit";
        }
        return " ".repeat(indent) + "if " + condition.print(indent) + " then " + thenBranch.print(indent) + "\n" + " ".repeat(indent) + "else " + elseBranch.print(indent);
    }
}