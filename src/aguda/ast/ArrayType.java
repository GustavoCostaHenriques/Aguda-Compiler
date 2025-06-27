package aguda.ast;

public class ArrayType extends AbstractAstNode {
    public final AstNode basicType;
    public final int dimensions;

    public ArrayType(int line, int column, AstNode basicType, int dimensions) {
        super(line, column);
        this.basicType = basicType;
        this.dimensions = dimensions;
    }

    @Override
    public String print(int indent) {
        StringBuilder dimensionsBuilder = new StringBuilder();
        for (int i = 0; i < this.dimensions; i++) {
            dimensionsBuilder.append("[]");
        }
        return basicType.print(indent) + dimensionsBuilder.toString();
    }
}
