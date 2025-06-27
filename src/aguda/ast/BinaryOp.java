package aguda.ast;

public class BinaryOp extends AbstractAstNode {
    public final String op;
    public final AstNode left;
    public final AstNode right;

    public BinaryOp(int line, int column, String op, AstNode left, AstNode right) {
        super(line, column);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String print(int indent) {
        // Print the operation with the operator in the middle and the operands enclosed with parentheses (eg. ( a ) + ( b ))
        return left.print(indent) + " " + op + " " + right.print(indent);
    }
}