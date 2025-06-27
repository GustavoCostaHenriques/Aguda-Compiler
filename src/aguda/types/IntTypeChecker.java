package aguda.types;

public class IntTypeChecker implements Type {
    @Override
    public boolean equals(Object other) {
        return other instanceof IntTypeChecker;
    }

    @Override
    public String toString() {
        return "Int";
    }
}
