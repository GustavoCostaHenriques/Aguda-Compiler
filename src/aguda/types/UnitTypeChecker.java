package aguda.types;

public class UnitTypeChecker implements Type {
    @Override
    public boolean equals(Object other) {
        return other instanceof UnitTypeChecker;
    }

    @Override
    public String toString() {
        return "Unit";
    }
}
