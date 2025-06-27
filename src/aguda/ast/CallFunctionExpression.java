package aguda.ast;

public class CallFunctionExpression extends AbstractAstNode {
    public final String id;
    public final AstNode exprs;
    
    public CallFunctionExpression(int line, int column, String id, AstNode exprs) {
        super(line, column);
        this.id = id;
        this.exprs = exprs;
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + id + "(" + exprs.print(indent) + ")";
    }
}