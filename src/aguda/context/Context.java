package aguda.context;

import aguda.types.Type;

import java.util.*;

public class Context {

    private final Deque<Map<String, Type>> scopes;

    public Context() {
        scopes = new ArrayDeque<>();
        beginScope(); // inicia com escopo global
    }

    public void beginScope() {
        scopes.push(new HashMap<>());
    }

    public void endScope() {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("No scope to end.");
        }
        scopes.pop();
    }

    public void add(String id, Type type) {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("No scope to insert into.");
        }
        scopes.peek().put(id, type);
    }

    public boolean contains(String id) {
        for (Map<String, Type> scope : scopes) {
            if (scope.containsKey(id)) return true;
        }
        return false;
    }

    public Type get(String id) {
        for (Map<String, Type> scope : scopes) {
            if (scope.containsKey(id)) {
                return scope.get(id);
            }
        }
        return null;
    }

    public void remove(String id) {
        if (!scopes.isEmpty()) {
            scopes.peek().remove(id);
        }
    }
}