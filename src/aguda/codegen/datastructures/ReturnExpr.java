package aguda.codegen.datastructures;

import aguda.types.*;

public class ReturnExpr {
    private String code;
    private String value;
    private Type type;
    private String label;

    public ReturnExpr(String code, String value, Type type, String label) {
        this.code = code;
        this.value = value;
        this.type = type;
        this.label = label;
    }

    /* --- Getters --- */
    public String getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }
}