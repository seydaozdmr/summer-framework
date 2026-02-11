package io.summerframework.core.context;

import io.summerframework.core.annotation.Autowired;
import io.summerframework.core.annotation.Bean;
import io.summerframework.core.annotation.Component;
import io.summerframework.core.annotation.ComponentScan;
import io.summerframework.core.annotation.Configuration;
import io.summerframework.core.annotation.Scope;
import io.summerframework.core.beans.BeanDefinition;
import io.summerframework.core.beans.BeanFactory;
import io.summerframework.core.lifecycle.BeanNameAware;
import io.summerframework.core.lifecycle.BeanPostProcessor;
import io.summerframework.core.lifecycle.DisposableBean;
import io.summerframework.core.lifecycle.InitializingBean;
import io.summerframework.core.web.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationApplicationContext implements BeanFactory, AutoCloseable {

    private static final String SINGLETON = "singleton";
    private static final String PROTOTYPE = "prototype";

    private final Map<String, BeanDefinition> beanDefinitions = new LinkedHashMap<>();
    private final Map<String, Object> singletonObjects = new HashMap<>();
    private final Set<String> beansInCreation = new HashSet<>();
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    private final ClassPathScanner scanner = new ClassPathScanner();

    public AnnotationApplicationContext(Class<?>... configurationClasses) {
        if (configurationClasses == null || configurationClasses.length == 0) {
            throw new IllegalArgumentException("At least one @Configuration class is required");
        }

        registerConfigurationAndScannedComponents(configurationClasses);
        refresh();
    }

    private void registerConfigurationAndScannedComponents(Class<?>[] configurationClasses) {
        for (Class<?> configClass : configurationClasses) {
            if (!configClass.isAnnotationPresent(Configuration.class)) {
                throw new IllegalArgumentException(configClass.getName() + " must be annotated with @Configuration");
            }

            registerBeanDefinition(new BeanDefinition(decapitalize(configClass.getSimpleName()), configClass, SINGLETON));
            registerBeanMethods(configClass);

            for (String basePackage : resolveBasePackages(configClass)) {
                for (Class<?> candidate : scanner.scan(basePackage)) {
                    if (!isManagedType(candidate)) {
                        continue;
                    }
                    String name = resolveComponentName(candidate);
                    String scope = resolveScope(candidate.getAnnotation(Scope.class));
                    registerBeanDefinition(new BeanDefinition(name, candidate, scope));
                }
            }
        }
    }

    private void registerBeanMethods(Class<?> configurationClass) {
        for (Method method : configurationClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Bean.class)) {
                continue;
            }
            String name = method.getAnnotation(Bean.class).value();
            if (name.isBlank()) {
                name = method.getName();
            }
            String scope = resolveScope(method.getAnnotation(Scope.class));
            BeanDefinition definition = new BeanDefinition(name, method.getReturnType(), scope, configurationClass, method);
            registerBeanDefinition(definition);
        }
    }

    private List<String> resolveBasePackages(Class<?> configurationClass) {
        ComponentScan componentScan = configurationClass.getAnnotation(ComponentScan.class);
        if (componentScan != null && componentScan.basePackages().length > 0) {
            return Arrays.asList(componentScan.basePackages());
        }
        Package configPackage = configurationClass.getPackage();
        return List.of(configPackage != null ? configPackage.getName() : "");
    }

    private String resolveComponentName(Class<?> candidate) {
        if (candidate.isAnnotationPresent(Component.class)) {
            String explicitName = candidate.getAnnotation(Component.class).value();
            if (!explicitName.isBlank()) {
                return explicitName;
            }
        }
        return decapitalize(candidate.getSimpleName());
    }

    private boolean isManagedType(Class<?> candidate) {
        return candidate.isAnnotationPresent(Component.class) || candidate.isAnnotationPresent(RestController.class);
    }

    private String resolveScope(Scope scope) {
        if (scope == null || scope.value().isBlank()) {
            return SINGLETON;
        }

        String value = scope.value().toLowerCase();
        if (!SINGLETON.equals(value) && !PROTOTYPE.equals(value)) {
            throw new IllegalArgumentException("Unsupported scope: " + scope.value());
        }
        return value;
    }

    private void registerBeanDefinition(BeanDefinition definition) {
        BeanDefinition existing = beanDefinitions.get(definition.getName());
        if (existing != null) {
            throw new IllegalStateException("Duplicate bean name detected: " + definition.getName());
        }
        beanDefinitions.put(definition.getName(), definition);
    }

    public void refresh() {
        List<String> processorBeanNames = new ArrayList<>();
        for (BeanDefinition definition : beanDefinitions.values()) {
            if (BeanPostProcessor.class.isAssignableFrom(definition.getBeanClass())) {
                processorBeanNames.add(definition.getName());
            }
        }

        for (String beanName : processorBeanNames) {
            BeanPostProcessor processor = (BeanPostProcessor) getBean(beanName);
            beanPostProcessors.add(processor);
        }

        for (BeanDefinition definition : beanDefinitions.values()) {
            if (definition.isSingleton()) {
                getBean(definition.getName());
            }
        }
    }

    @Override
    public Object getBean(String name) {
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition == null) {
            throw new IllegalStateException("No bean named '" + name + "' is defined");
        }

        if (definition.isSingleton()) {
            Object singleton = singletonObjects.get(name);
            if (singleton != null) {
                return singleton;
            }
            return createAndCacheSingleton(name, definition);
        }

        return createBean(name, definition);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(Class<T> requiredType) {
        List<String> matches = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
                matches.add(entry.getKey());
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalStateException("No bean found for type: " + requiredType.getName());
        }

        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple beans found for type " + requiredType.getName() + ": " + matches);
        }

        return (T) getBean(matches.get(0));
    }

    @Override
    public boolean containsBean(String name) {
        return beanDefinitions.containsKey(name);
    }

    public List<String> getBeanDefinitionNames() {
        return List.copyOf(beanDefinitions.keySet());
    }

    public Class<?> getBeanType(String name) {
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition == null) {
            throw new IllegalStateException("No bean named '" + name + "' is defined");
        }
        return definition.getBeanClass();
    }

    private Object createAndCacheSingleton(String beanName, BeanDefinition definition) {
        if (beansInCreation.contains(beanName)) {
            throw new IllegalStateException("Circular dependency detected while creating: " + beanName);
        }

        beansInCreation.add(beanName);
        try {
            Object bean = createBean(beanName, definition);
            singletonObjects.put(beanName, bean);
            return bean;
        } finally {
            beansInCreation.remove(beanName);
        }
    }

    private Object createBean(String beanName, BeanDefinition definition) {
        Object bean = instantiateBean(definition);
        injectAutowiredFields(bean);

        if (bean instanceof BeanNameAware beanNameAware) {
            beanNameAware.setBeanName(beanName);
        }

        Object initializedBean = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            initializedBean = processor.postProcessBeforeInitialization(initializedBean, beanName);
        }

        if (initializedBean instanceof InitializingBean initializingBean) {
            initializingBean.afterPropertiesSet();
        }

        for (BeanPostProcessor processor : beanPostProcessors) {
            initializedBean = processor.postProcessAfterInitialization(initializedBean, beanName);
        }

        return initializedBean;
    }

    private Object instantiateBean(BeanDefinition definition) {
        if (definition.isFactoryMethodBean()) {
            return instantiateFromFactoryMethod(definition);
        }

        Constructor<?> constructor = resolveConstructor(definition.getBeanClass());
        Object[] args = resolveExecutableArguments(constructor.getParameters());

        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to instantiate bean class: " + definition.getBeanClass().getName(), ex);
        }
    }

    private Object instantiateFromFactoryMethod(BeanDefinition definition) {
        Object configInstance = getBean(definition.getConfigurationClass());
        Method factoryMethod = definition.getFactoryMethod();
        Object[] args = resolveExecutableArguments(factoryMethod.getParameters());

        try {
            factoryMethod.setAccessible(true);
            return factoryMethod.invoke(configInstance, args);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to invoke @Bean method: " + factoryMethod, ex);
        }
    }

    private Constructor<?> resolveConstructor(Class<?> beanClass) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        List<Constructor<?>> autowired = new ArrayList<>();

        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                autowired.add(constructor);
            }
        }

        if (autowired.size() > 1) {
            throw new IllegalStateException("Multiple @Autowired constructors found in " + beanClass.getName());
        }

        if (autowired.size() == 1) {
            return autowired.get(0);
        }

        if (constructors.length == 1) {
            return constructors[0];
        }

        try {
            return beanClass.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("No suitable constructor found for " + beanClass.getName(), ex);
        }
    }

    private Object[] resolveExecutableArguments(Parameter[] parameters) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = getBean(parameters[i].getType());
        }
        return args;
    }

    private void injectAutowiredFields(Object bean) {
        Class<?> current = bean.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }

                Object dependency = getBean(field.getType());
                try {
                    field.setAccessible(true);
                    field.set(bean, dependency);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Failed to inject field " + field.getName() + " on " + bean.getClass().getName(), ex);
                }
            }
            current = current.getSuperclass();
        }
    }

    private String decapitalize(String simpleName) {
        if (simpleName == null || simpleName.isEmpty()) {
            return simpleName;
        }
        if (simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(1)) && Character.isUpperCase(simpleName.charAt(0))) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    @Override
    public void close() {
        Collection<Object> beans = new ArrayList<>(singletonObjects.values());
        for (Object bean : beans) {
            if (bean instanceof DisposableBean disposableBean) {
                disposableBean.destroy();
            }
        }
        singletonObjects.clear();
        beanPostProcessors.clear();
        beanDefinitions.clear();
    }
}
