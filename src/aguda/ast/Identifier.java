package aguda.ast;

public class Identifier extends AbstractAstNode {
    public final String value;

    public Identifier(int line, int column, String value) {
        super(line, column);
        this.value = value;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + value;
    }
}