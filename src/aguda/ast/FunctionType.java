package aguda.ast;

public class FunctionType extends AbstractAstNode {
    public final AstNode typeList;
    public final AstNode returnType;

    public FunctionType(int line, int column, AstNode typeList, AstNode returnType) {
        super(line, column);
        this.typeList = typeList;
        this.returnType = returnType;
    }

    @Override
    public String print(int indent) {
            
        return typeList.print(indent) + " -> " + returnType.print(indent);
    }
}