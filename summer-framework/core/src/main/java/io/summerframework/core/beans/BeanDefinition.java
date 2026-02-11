package io.summerframework.core.beans;

import java.lang.reflect.Method;

public class BeanDefinition {

    private final String name;
    private final Class<?> beanClass;
    private final String scope;
    private final Class<?> configurationClass;
    private final Method factoryMethod;

    public BeanDefinition(String name, Class<?> beanClass, String scope) {
        this(name, beanClass, scope, null, null);
    }

    public BeanDefinition(String name, Class<?> beanClass, String scope, Class<?> configurationClass, Method factoryMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.scope = scope;
        this.configurationClass = configurationClass;
        this.factoryMethod = factoryMethod;
    }

    public String getName() {
        return name;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getScope() {
        return scope;
    }

    public boolean isSingleton() {
        return "singleton".equals(scope);
    }

    public boolean isPrototype() {
        return "prototype".equals(scope);
    }

    public boolean isFactoryMethodBean() {
        return configurationClass != null && factoryMethod != null;
    }

    public Class<?> getConfigurationClass() {
        return configurationClass;
    }

    public Method getFactoryMethod() {
        return factoryMethod;
    }
}
