package aguda.types;

public class BoolTypeChecker implements Type {
    @Override
    public boolean equals(Object other) {
        return other instanceof BoolTypeChecker;
    }

    @Override
    public String toString() {
        return "Bool";
    }
}
