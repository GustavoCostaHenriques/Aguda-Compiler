package aguda.ast;

public abstract class AbstractAstNode implements AstNode {
    protected final int line;
    protected final int column;

    public AbstractAstNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }
}
