package aguda.ast;

public interface AstNode {
    String print(int indent);
    int getLine();
    int getColumn();
}