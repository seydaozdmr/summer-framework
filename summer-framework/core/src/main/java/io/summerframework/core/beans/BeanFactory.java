package io.summerframework.core.beans;

public interface BeanFactory {

    Object getBean(String name);

    <T> T getBean(Class<T> requiredType);

    boolean containsBean(String name);
}
