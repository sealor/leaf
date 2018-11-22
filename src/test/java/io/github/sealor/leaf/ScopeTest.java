package io.github.sealor.leaf;

import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ScopeTest {

    public interface FirstLetter {

    }

    public static class A implements FirstLetter {
    }

    public static class B {
        private final A a;

        public B(A a) {
            this.a = a;
        }
    }

    @Qualifier
    @Retention(RUNTIME)
    public @interface MyFirstLetter {
    }

    @Qualifier
    @Retention(RUNTIME)
    public @interface MyA {
    }

    public static class C {
        private final A a;

        public C(@MyA A a) {
            this.a = a;
        }
    }

    public static class D {
        private final A a;

        public D(@Named("A") A a) {
            this.a = a;
        }
    }

    public static class E {
        private final A a;

        public E() {
            this.a = null;
        }

        @Inject
        public E(A a) {
            this.a = a;
        }
    }

    @Test
    public void testResolveOneClass() {
        assertEquals(A.class, new Scope().resolve(A.class).getClass());
    }

    @Test
    public void testResolveClasseHierarchy() {
        Scope scope = new Scope();
        B b = scope.resolve(B.class);

        assertEquals(A.class, b.a.getClass());
    }

    @Test
    public void testScopedInstance() {
        Scope scope = new Scope();
        A a = new A();
        scope.putScopedInstance(A.class, a);
        B b1 = scope.resolve(B.class);
        B b2 = scope.resolve(B.class);

        assertNotEquals(b1, b2);
        assertEquals(b1.a, b2.a);
        assertEquals(a, b1.a);
    }

    @Test
    public void testScopedClass() {
        Scope scope = new Scope();
        scope.putScopedClass(A.class, A.class);
        B b1 = scope.resolve(B.class);
        B b2 = scope.resolve(B.class);

        assertNotEquals(b1, b2);
        assertEquals(b1.a, b2.a);
    }

    @Test
    public void testClassMapping() {
        Scope scope = new Scope();
        scope.putClassMapping(FirstLetter.class, A.class);
        FirstLetter letter = scope.resolve(FirstLetter.class);

        assertEquals(A.class, letter.getClass());
    }

    @Test
    public void testInstanceProvider() {
        Scope scope = new Scope();
        scope.putInstanceProvider(new Provider<FirstLetter>() {

            @Override
            public FirstLetter get() {
                return new A();
            }
        });
        FirstLetter letter = scope.resolve(FirstLetter.class);

        assertEquals(A.class, letter.getClass());
    }

    @Test
    public void testResolveScopedInstanceByAnnotation() {
        Scope scope = new Scope();
        A a = new A();
        scope.putScopedInstance(MyA.class, a);

        assertEquals(a, scope.resolve(C.class).a);
    }

    @Test
    public void testResolveScopedClassByAnnotation() {
        Scope scope = new Scope();
        scope.putScopedClass(MyA.class, A.class);
        A a = scope.resolve(C.class).a;

        assertEquals(a, scope.resolve(C.class).a);
    }

    @Test
    public void testClassMappingByAnnotation() {
        Scope scope = new Scope();
        A a = new A();
        scope.putScopedInstance(MyA.class, a);
        scope.putClassMapping(MyFirstLetter.class, MyA.class);

        assertEquals(A.class, scope.resolve(MyFirstLetter.class, A.class).getClass());
    }

    @Test
    public void testProviderByAnnotation() {
        Scope scope = new Scope();
        scope.putInstanceProvider(MyFirstLetter.class, new Provider<A>() {

            @Override
            public A get() {
                return new A();
            }
        });

        assertEquals(A.class, scope.resolve(MyFirstLetter.class, A.class).getClass());
    }

    @Test
    public void testResolveScopedInstanceByName() {
        Scope scope = new Scope();
        A a = new A();
        scope.putScopedInstance("A", a);

        assertEquals(a, scope.resolve(D.class).a);
    }

    @Test
    public void testResolveScopedClassByName() {
        Scope scope = new Scope();
        scope.putScopedClass("A", A.class);
        A a = scope.resolve(D.class).a;

        assertEquals(a, scope.resolve(D.class).a);
    }

    @Test
    public void testClassMappingByName() {
        Scope scope = new Scope();
        A a = new A();
        scope.putScopedInstance(A.class, a);
        scope.putClassMapping("A", A.class);

        assertEquals(A.class, scope.resolve("A", A.class).getClass());
    }

    @Test
    public void testProviderByName() {
        Scope scope = new Scope();
        scope.putInstanceProvider("A", new Provider<A>() {

            @Override
            public A get() {
                return new A();
            }
        });

        assertEquals(A.class, scope.resolve("A", A.class).getClass());
    }

    @Test
    public void testInjectAnnotation() {
        assertNotNull(new Scope().resolve(E.class).a);
    }

    @Test
    public void testSubScopeUsingScopedInstancesFromParentScope() {
        Scope parentScope = new Scope();
        A a = new A();
        parentScope.putScopedInstance(a);

        Scope subscope = new Scope(parentScope);
        assertEquals(a, subscope.resolve(B.class).a);
    }

    @Test
    public void testSubScopeUsingScopedClassFromParentScope() {
        Scope parentScope = new Scope();
        parentScope.putScopedClass(A.class);

        Scope subscope = new Scope(new Scope(parentScope));
        assertEquals(subscope.resolve(B.class).a, subscope.resolve(B.class).a);
    }

}
