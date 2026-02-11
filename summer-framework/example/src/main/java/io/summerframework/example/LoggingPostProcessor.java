package io.summerframework.example;

import io.summerframework.core.annotation.Component;
import io.summerframework.core.lifecycle.BeanPostProcessor;

@Component
public class LoggingPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("[bpp-before] " + beanName + " -> " + bean.getClass().getSimpleName());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("[bpp-after] " + beanName + " -> " + bean.getClass().getSimpleName());
        return bean;
    }
}
