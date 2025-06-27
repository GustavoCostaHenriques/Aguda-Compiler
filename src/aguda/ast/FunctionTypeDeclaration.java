package aguda.ast;

import java.util.List;

public class FunctionTypeDeclaration extends AbstractAstNode {
    public final AstNode idList;
    public final AstNode functionType;
    public final AstNode exprs;

    public FunctionTypeDeclaration(int line, int column, AstNode idList, AstNode functionType, AstNode exprs) {
        super(line, column);
        this.idList = idList;
        this.functionType = functionType;
        this.exprs = exprs;
    }

    @Override
    public String print(int indent) {
        return "let " + idList.print(indent) + " : " + functionType.print(indent) + " =\n" + exprs.print(indent + 2);
    }
}
