package aguda.ast;

import java.util.List;

public class ArrayCreation extends AbstractAstNode {
    public final String baseType;
    private final List<Dimension> dimensions;

    public ArrayCreation(int line, int column, String baseType, List<Dimension> dimensions) {
        super(line, column);
        this.baseType = baseType;
        this.dimensions = dimensions;
    }

    public static class Dimension {
        public final AstNode sizeExpr;
        public final AstNode initExpr;

        public Dimension(AstNode sizeExpr, AstNode initExpr) {
            this.sizeExpr = sizeExpr;
            this.initExpr = initExpr;
        }
    }

    @Override
    public String print(int indent) {
        StringBuilder sb = new StringBuilder(" ".repeat(indent)).append("new ").append(baseType);
        for (Dimension dim : dimensions) {
            sb.append("[");
            if (dim.sizeExpr != null && dim.initExpr != null) {
                sb.append(dim.sizeExpr.print(indent))
                .append(" | ").append(dim.initExpr.print(indent));
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public List<Dimension> getDimensions() {
        return dimensions;
    }
}
