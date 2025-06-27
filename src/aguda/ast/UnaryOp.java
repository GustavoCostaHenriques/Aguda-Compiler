package aguda.ast;

public class UnaryOp extends AbstractAstNode {
    public final String op;
    public final AstNode expr;

    public UnaryOp(int line, int column, String op, AstNode expr) {
        super(line, column);
        this.op = op;
        this.expr = expr;
    }

    @Override
    public String print(int indent) {
        if(op.equals("-")) return " ".repeat(indent) + "0 - " + expr.print(indent);
        return " ".repeat(indent) + op + expr.print(indent);
    }
}