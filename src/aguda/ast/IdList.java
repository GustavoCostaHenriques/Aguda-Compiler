package aguda.ast;

import java.util.List;

public class IdList extends AbstractAstNode {
    public final String id;
    public final List<AstNode> ids;

    public IdList(int line, int column, String id, List<AstNode> ids) {
        super(line, column);
        this.id = id;
        this.ids = ids;
    }

    @Override
    public String print(int indent) {
        StringBuilder sb = new StringBuilder(id + "(");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i).print(0)); // no internal indent
            if (i < ids.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
