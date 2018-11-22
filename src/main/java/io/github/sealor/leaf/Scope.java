package io.github.sealor.leaf;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Scope {

    protected Scope parentScope;

    protected Map<Object, Object> scopedInstances = new HashMap<>();
    protected Map<Object, Class<?>> scopedClasses = new HashMap<>();
    protected Map<Object, Class<?>> classMapping = new HashMap<>();

    protected Map<Object, Provider<?>> instanceProviders = new HashMap<>();

    public Scope() {
    }

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
    }

    public void putScopedInstance(Object instance) {
        putScopedInstance(instance.getClass(), instance);
    }

    public void putScopedInstance(Class<?> interfaceOrAnnotationClass, Object instance) {
        this.scopedInstances.put(interfaceOrAnnotationClass, instance);
    }

    public void putScopedInstance(String instanceName, Object instance) {
        this.scopedInstances.put(instanceName, instance);
    }

    public void putScopedClass(Class<?> interfaceOrAnnotationClass) {
        this.scopedClasses.put(interfaceOrAnnotationClass, interfaceOrAnnotationClass);
    }

    public void putScopedClass(Class<?> interfaceOrAnnotationClass, Class<?> instanceClass) {
        this.scopedClasses.put(interfaceOrAnnotationClass, instanceClass);
    }

    public void putScopedClass(String instanceName, Class<?> instanceClass) {
        this.scopedClasses.put(instanceName, instanceClass);
    }

    public void putClassMapping(Class<?> interfaceOrAnnotationClass, Class<?> instanceClass) {
        this.classMapping.put(interfaceOrAnnotationClass, instanceClass);
    }

    public void putClassMapping(String instanceName, Class<?> instanceClass) {
        this.classMapping.put(instanceName, instanceClass);
    }

    public void putInstanceProvider(Class<?> interfaceOrAnnotationClass, Provider<?> instanceProvider) {
        this.instanceProviders.put(interfaceOrAnnotationClass, instanceProvider);
    }

    public void putInstanceProvider(String instanceName, Provider<?> instanceProvider) {
        this.instanceProviders.put(instanceName, instanceProvider);
    }

    public void putInstanceProvider(Provider<?> instanceProvider) {
        ParameterizedType providerType = (ParameterizedType) instanceProvider.getClass().getGenericInterfaces()[0];
        Type instanceType = providerType.getActualTypeArguments()[0];

        this.instanceProviders.put(instanceType, instanceProvider);
    }

    public <T> T resolve(Class<T> interfaceClass) {
        return resolve(interfaceClass, interfaceClass);
    }

    public <T> T resolve(Object instanceKey, Class<T> interfaceClass) {
        try {
            T instanceFromCurrentScope = resolveWithinCurrentAndUpperScopes(instanceKey, interfaceClass);
            if (instanceFromCurrentScope != null)
                return instanceFromCurrentScope;

            return instantiateClass(instanceKey);
        } catch (ScopeException e) {
            throw new ScopeException("Resolving failed: " + instanceKey, e);
        }
    }


    protected <T> T resolveWithinCurrentAndUpperScopes(Object instanceKey, Class<T> interfaceClass) {
        T scopedInstanceFromParentScope = resolveWithParentScopes(instanceKey, interfaceClass);
        if (scopedInstanceFromParentScope != null)
            return scopedInstanceFromParentScope;

        @SuppressWarnings("unchecked")
        T scopedInstance = (T) this.scopedInstances.get(instanceKey);
        if (scopedInstance != null)
            return scopedInstance;

        @SuppressWarnings("unchecked")
        Class<T> scopedInstanceClass = (Class<T>) this.scopedClasses.get(instanceKey);
        if (scopedInstanceClass != null)
            return instantiateScopedClass(instanceKey, scopedInstanceClass);

        @SuppressWarnings("unchecked")
        Class<T> mappingTargetClass = (Class<T>) this.classMapping.get(instanceKey);
        if (mappingTargetClass != null)
            return resolveWithinCurrentAndUpperScopes(mappingTargetClass, mappingTargetClass);

        return null;
    }

    protected <T> T resolveWithParentScopes(Object instanceKey, Class<T> interfaceClass) {
        if (this.parentScope == null)
            return null;

        return this.parentScope.resolveWithinCurrentAndUpperScopes(instanceKey, interfaceClass);
    }

    protected <T> T instantiateScopedClass(Object instanceKey, Class<T> scopedInstanceClass) {
        T newInstance = instantiateClass(scopedInstanceClass);

        if (this.scopedInstances.containsKey(instanceKey))
            throw new ScopeException("Circular dependency graph detected: " + instanceKey.getClass().getName());

        this.scopedInstances.put(instanceKey, newInstance);
        return newInstance;
    }

    protected <T> T instantiateClass(Object instanceKey) {
        @SuppressWarnings("unchecked")
        Provider<T> instanceProvider = (Provider<T>) this.instanceProviders.get(instanceKey);
        if (instanceProvider != null)
            return instanceProvider.get();

        @SuppressWarnings("unchecked")
        Class<T> mappingTargetClass = (Class<T>) this.classMapping.get(instanceKey);
        if (mappingTargetClass != null)
            return instantiateClass(mappingTargetClass);

        if (instanceKey instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<T> instanceClass = (Class<T>) instanceKey;
            return instantiateClassWithReflection(instanceClass);
        }

        throw new ScopeException("Unknown instance key: " + instanceKey);
    }

    protected <T> T instantiateClassWithReflection(Class<T> instanceClass) {
        Constructor<T> constructor = resolveConstructor(instanceClass);
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
            parameters[i] = instantiateParameterValue(parameterTypes[i], parameterAnnotations[i]);

        return newInstance(constructor, parameters);
    }

    protected <T> Constructor<T> resolveConstructor(Class<T> instanceClass) {
        if (instanceClass.getConstructors().length == 0)
            throw new ScopeException("No constructors found: " + instanceClass.getName());

        @SuppressWarnings("unchecked")
        Constructor<T>[] constructors = (Constructor<T>[]) instanceClass.getConstructors();

        if (constructors.length == 1)
            return constructors[0];

        for (Constructor<T> constructor : constructors)
            if (constructor.isAnnotationPresent(Inject.class))
                return constructor;

        throw new ScopeException("No constructor with @Inject found: " + instanceClass.getName());
    }

    protected Object instantiateParameterValue(Class<?> parameterType, Annotation[] parameterAnnotations) {
        for (Annotation annotation : parameterAnnotations)
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class))
                if (annotation.annotationType() == Named.class)
                    return resolve(((Named) annotation).value(), Object.class);
                else
                    return resolve(annotation.annotationType());

        return resolve(parameterType);
    }

    protected <T> T newInstance(Constructor<T> constructor, Object... parameters) {
        try {
            return constructor.newInstance(parameters);
        } catch (Exception e) {
            throw new ScopeException(e);
        }
    }

}
