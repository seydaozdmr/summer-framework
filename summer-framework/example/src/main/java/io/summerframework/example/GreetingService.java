package io.summerframework.example;

import io.summerframework.core.annotation.Component;
import io.summerframework.core.lifecycle.BeanNameAware;
import io.summerframework.core.lifecycle.InitializingBean;

@Component
public class GreetingService implements BeanNameAware, InitializingBean {

    private String beanName;

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("[init] " + beanName + " initialized");
    }

    public String greet(String name) {
        return "Hello " + name + " from " + beanName;
    }
}
