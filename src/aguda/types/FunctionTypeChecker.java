package aguda.types;

public class FunctionTypeChecker implements Type {
    private final Type from;
    private final Type to;

    public FunctionTypeChecker(Type from, Type to) {
        this.from = from;
        this.to = to;
    }

    public Type getFrom() {
        return from;
    }

    public Type getTo() {
        return to;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FunctionTypeChecker ft) {
            return from.equals(ft.from) && to.equals(ft.to);
        }
        return false;
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
