package aguda.codegen.datastructures;

import aguda.types.*;

public class Ctx {
    private Type type;
    private String reg;
    private String ptr;
    private boolean isFunction;
    private String insideFunction;

    public Ctx(Type type, String reg, String ptr, boolean isFunction, String insideFunction) {
        this.type = type;
        this.reg = reg;
        this.ptr = ptr;
        this.isFunction = isFunction;
        this.insideFunction = insideFunction;
    }

    /* --- Getters --- */
    public Type getType() {
        return type;
    }

    public String getReg() {
        return reg;
    }

    public String getPtr() {
        return ptr;
    }

    public boolean getIsFunction() {
        return isFunction;
    }

    public String getInsideFunction() {
        return insideFunction;
    }

    /* --- Setters --- */
    public void setReg(String reg) {
        this.reg = reg;
    }
}