package aguda.types;

public class ArrayTypeChecker implements Type {
    private final Type elementType;
    private final int dimensions;

    public ArrayTypeChecker(Type elementType, int dimensions) {
        this.elementType = elementType;
        this.dimensions = dimensions;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getDimensions() {
        return dimensions;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArrayTypeChecker at && elementType.equals(at.getElementType())
                && dimensions == at.getDimensions();
    }

    @Override
    public String toString() {
        return elementType.toString() + "[]".repeat(dimensions);
    }
}