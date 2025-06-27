package aguda.ast;

import java.util.List;

public class ArrayAccess extends AbstractAstNode {
    public final AstNode array;
    public final List<AstNode> indices;

    public ArrayAccess(int line, int column, AstNode array, List<AstNode> indices) {
        super(line, column);
        this.array = array;
        this.indices = indices;
    }

    @Override
    public String print(int indent) {
        StringBuilder sb = new StringBuilder(" ".repeat(indent)).append(array.print(indent));
        for (AstNode index : indices) {
            if (index instanceof Expressions) {
                sb.append("[");
                sb.append(index.print(indent));
                sb.append("]");
            } else {
                sb.append("[").append(index.print(indent)).append("]");
            }
        }
        return sb.toString();
    }
}
