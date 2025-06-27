package aguda.ast;

public class LetExpression extends AbstractAstNode {
    public final String id;
    public final AstNode type;
    public final AstNode blockExpr;

    public LetExpression(int line, int column, String id, AstNode type, AstNode blockExpr) {
        super(line, column);
        this.id = id;
        this.type = type;
        this.blockExpr = blockExpr;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + "let " + id + " : " + type.print(indent) + " =\n" + blockExpr.print(indent + 2);
    }
}
