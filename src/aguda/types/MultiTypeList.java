package aguda.types;

import java.util.*;

public class MultiTypeList implements Type {
    private final List<Type> types;

    public MultiTypeList(List<Type> types) {
        this.types = types;
    }

    public List<Type> getTypes() {
        return types;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MultiTypeList mt) {
            return types.equals(mt.types);
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + String.join(", ", types.stream().map(Type::toString).toList()) + ")";
    }
}