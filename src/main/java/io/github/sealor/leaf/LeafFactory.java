package io.github.sealor.leaf;

public class LeafFactory {

    public Scope createRootScope() {
        return new Scope();
    }

    public Scope createSubScope(Scope parentScope) {
        return new Scope(parentScope);
    }

}
