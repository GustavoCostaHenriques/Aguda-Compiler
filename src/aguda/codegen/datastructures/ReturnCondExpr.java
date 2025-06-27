package aguda.codegen.datastructures;

public class ReturnCondExpr {
    private String code;
    private String label;

    public ReturnCondExpr(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /* --- Getters --- */
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}