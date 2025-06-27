package aguda.ast;

import java.math.BigInteger;

public class IntLiteral extends AbstractAstNode {
    public final String value;

    public IntLiteral(int line, int column, String value) {
        super(line, column);
        this.value = value;
    }

    public BigInteger toBigInteger() {
        return new BigInteger(value);
    }

    @Override
    public String print(int indent) {
        return " ".repeat(indent) + value;
    }
}
