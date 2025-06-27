package aguda.ast;

public class BoolLiteral extends AbstractAstNode {
    public final boolean value;

    public BoolLiteral(int line, int column,boolean value) {
        super(line, column);
        this.value = value;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + (value ? "true" : "false");
    }
}