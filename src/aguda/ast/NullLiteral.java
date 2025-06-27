package aguda.ast;

public class NullLiteral extends AbstractAstNode {

    public NullLiteral(int line, int column) {
        super(line, column);
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + "unit";
    }
}