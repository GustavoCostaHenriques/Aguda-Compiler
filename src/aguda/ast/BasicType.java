package aguda.ast;

public class BasicType extends AbstractAstNode {
    public final String basicType;

    public BasicType(int line, int column, String basicType) {
        super(line, column);
        this.basicType = basicType;
    }

    @Override
    public String print(int indent) {
        return basicType;
    }
}