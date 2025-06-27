package aguda.ast;

public class SetExpression extends AbstractAstNode {
    public final AstNode lhs;
    public final AstNode value;

    public SetExpression(int line, int column, AstNode lhs, AstNode value) {
        super(line, column);
        this.lhs = lhs;
        this.value = value;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + "set " + lhs.print(indent) + " =\n" + value.print(indent + 2);
    }
}
