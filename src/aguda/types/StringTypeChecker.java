package aguda.types;

public class StringTypeChecker implements Type {
    @Override
    public boolean equals(Object other) {
        return other instanceof StringTypeChecker;
    }

    @Override
    public String toString() {
        return "String";
    }
}
