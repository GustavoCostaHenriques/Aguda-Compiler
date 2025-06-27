package aguda.types;

public class UndeclaredTypeChecker implements Type {
    @Override
    public boolean equals(Object other) {
        return other instanceof UndeclaredTypeChecker;
    }

    @Override
    public String toString() {
        return "Undeclared";
    }
}