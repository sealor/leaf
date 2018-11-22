package io.github.sealor.leaf;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LeafFactoryTest {

    @Test
    public void testCreateRootScope() {
        Scope scope = new LeafFactory().createRootScope();

        assertNull(scope.parentScope);
    }

    @Test
    public void testSubScope() {
        LeafFactory leafFactory = new LeafFactory();

        Scope parentScope = leafFactory.createRootScope();
        Scope scope = leafFactory.createSubScope(parentScope);

        assertEquals(scope.parentScope, parentScope);
    }

}
