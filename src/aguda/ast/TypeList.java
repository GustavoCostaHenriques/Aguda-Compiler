package aguda.ast;

import java.util.List;

public class TypeList extends AbstractAstNode {
    public final List<AstNode> typesParam;

    public TypeList(int line, int column, List<AstNode> typesParam) {
        super(line, column);
        this.typesParam = typesParam;
    }

    @Override
    public String print(int indent) {
        if (typesParam.size() == 1) {
            return typesParam.get(0).print(indent);
        } else {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < typesParam.size(); i++) {
                sb.append(typesParam.get(i).print(0)); // no internal indent
                if (i < typesParam.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return " ".repeat(indent) + sb.toString();
        }
    }

    public List<AstNode> getTypes() {
        return typesParam;
    }
}