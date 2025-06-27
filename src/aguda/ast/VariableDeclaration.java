package aguda.ast;

import java.util.List;

public class VariableDeclaration extends AbstractAstNode {
    public final String id;
    public final AstNode typeElem;
    public final AstNode exprs;

    public VariableDeclaration(int line, int column, String id, AstNode typeElem, AstNode exprs) {
        super(line, column);
        this.id = id;
        this.typeElem = typeElem;
        this.exprs = exprs;
    }

    @Override
    public String print(int indent) {
        return "let " + id + " : " + typeElem.print(indent) + " =\n" + exprs.print(indent + 2);
    }
}