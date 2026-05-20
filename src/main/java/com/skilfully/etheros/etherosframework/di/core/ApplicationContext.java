package com.skilfully.etheros.etherosframework.di.core;

import com.skilfully.etheros.etherosframework.di.annotation.*;
import com.skilfully.etheros.etherosframework.di.exception.BeanCreationException;
import com.skilfully.etheros.etherosframework.di.exception.BeanNotFoundException;
import com.skilfully.etheros.etherosframework.di.lifecycle.LifecycleProcessor;
import com.skilfully.etheros.etherosframework.di.scanner.ClassPathScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationContext {

    private static final Logger LOG = Logger.getLogger("EtherosFramework-Context");

    private final BeanRegistry registry = new BeanRegistry();
    private final List<BeanDefinition> definitions = new ArrayList<>();
    private final LifecycleProcessor lifecycleProcessor = new LifecycleProcessor();
    private ClassLoader classLoader;
    private String basePackage;
    private PropertyLoader propertyLoader;

    public ApplicationContext() {
    }

    public static ApplicationContext run(Class<?> primarySource) {
        ApplicationContext ctx = new ApplicationContext();
        ctx.classLoader = primarySource.getClassLoader();
        ctx.basePackage = primarySource.getPackage().getName();
        ctx.propertyLoader = new PropertyLoader(ctx.classLoader);
        ctx.scan();
        ctx.refresh();
        return ctx;
    }

    public static ApplicationContext run(Class<?> primarySource, Object... externalBeans) {
        ApplicationContext ctx = run(primarySource);
        for (Object bean : externalBeans) {
            ctx.registerSingleton(bean.getClass(), bean);
        }
        return ctx;
    }

    void scan() {
        ClassPathScanner scanner = new ClassPathScanner(classLoader);
        Set<String> candidates = scanner.scan(basePackage);
        for (String className : candidates) {
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                definitions.add(BeanDefinition.from(clazz));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load class: " + className, e);
            }
        }
    }

    void refresh() {
        instantiateSingletons();
        injectFields();
        invokeBeanFactories();
        invokePostConstructs();
        registerGlobalBeans();
    }

    private void instantiateSingletons() {
        int remaining;
        int maxPasses = definitions.size() + 1;
        for (int pass = 0; pass < maxPasses; pass++) {
            remaining = 0;
            for (BeanDefinition def : definitions) {
                if (def.isPrototype()) continue;
                if (registry.getByName(def.getBeanName()) != null) continue;
                try {
                    Object instance = createInstance(def);
                    registry.register(def.getBeanName(), def.getBeanClass(), instance);
                } catch (BeanNotFoundException e) {
                    remaining++;
                } catch (Exception e) {
                    throw new BeanCreationException(
                            "Failed to instantiate " + def.getBeanClass().getName(), e);
                }
            }
            if (remaining == 0) break;
        }
        checkUninstantiated(definitions);
    }

    private void checkUninstantiated(List<BeanDefinition> definitions) {
        List<String> unresolved = new ArrayList<>();
        for (BeanDefinition def : definitions) {
            if (def.isPrototype()) continue;
            if (registry.getByName(def.getBeanName()) == null) {
                unresolved.add(def.getBeanClass().getName());
            }
        }
        if (!unresolved.isEmpty()) {
            throw new BeanCreationException(
                    "Unresolved dependencies for beans: " + String.join(", ", unresolved));
        }
    }

    private Object createInstance(BeanDefinition def) throws Exception {
        Constructor<?> ctor = def.getConstructor();
        Class<?>[] paramTypes = ctor.getParameterTypes();
        if (paramTypes.length == 0) {
            return ctor.newInstance();
        }
        Object[] params = resolveParams(paramTypes,
                ctor.getParameterAnnotations(), def.getBeanClass().getName());
        return ctor.newInstance(params);
    }

    private Object[] resolveParams(Class<?>[] paramTypes, java.lang.annotation.Annotation[][] paramAnnos,
                                   String contextName) {
        Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            params[i] = resolveParameter(paramTypes[i], paramAnnos[i], contextName);
        }
        return params;
    }

    private Object resolveParameter(Class<?> type, java.lang.annotation.Annotation[] annotations,
                                    String contextName) {
        for (java.lang.annotation.Annotation ann : annotations) {
            if (ann instanceof Value) {
                String value = propertyLoader.getProperty(((Value) ann).value());
                if (value == null) {
                    throw new BeanCreationException(
                            "Property not found: " + ((Value) ann).value() + " for " + contextName);
                }
                return convertValue(value, type);
            }
            if (ann instanceof GlobalAutowired) {
                GlobalAutowired ga = (GlobalAutowired) ann;
                return resolveGlobalBean(ga.value(), type, ga.required(), "constructor param in " + contextName);
            }
            if (ann instanceof Autowired) {
                Autowired a = (Autowired) ann;
                return resolveLocalBean(a.value(), type, a.required(), "constructor param in " + contextName);
            }
        }
        Object bean = registry.getByType(type);
        if (bean != null) return bean;
        bean = SharedContext.getBean(type);
        if (bean != null) return bean;
        throw new BeanNotFoundException(type);
    }

    private void injectFields() {
        for (BeanDefinition def : definitions) {
            if (def.isPrototype()) continue;
            Object instance = registry.getByName(def.getBeanName());
            if (instance == null) continue;
            for (Field field : def.getInjectFields()) {
                try {
                    Object value = resolveFieldValue(field, def.getBeanClass().getName());
                    field.set(instance, value);
                } catch (Exception e) {
                    throw new BeanCreationException(
                            "Failed to inject " + field.getName() + " on "
                                    + def.getBeanClass().getName(), e);
                }
            }
        }
    }

    private Object resolveFieldValue(Field field, String contextName) {
        Value valueAnn = field.getAnnotation(Value.class);
        if (valueAnn != null) {
            String raw = propertyLoader.getProperty(valueAnn.value());
            if (raw == null) {
                throw new BeanCreationException(
                        "Property not found: " + valueAnn.value() + " for " + contextName + "." + field.getName());
            }
            return convertValue(raw, field.getType());
        }

        GlobalAutowired globalAnn = field.getAnnotation(GlobalAutowired.class);
        if (globalAnn != null) {
            Object bean = resolveGlobalBean(globalAnn.value(), field.getType(), globalAnn.required(), field.getName());
            return bean;
        }

        Autowired autowireAnn = field.getAnnotation(Autowired.class);
        if (autowireAnn != null) {
            Object bean = resolveLocalBean(autowireAnn.value(), field.getType(), autowireAnn.required(), field.getName());
            return bean;
        }

        return null;
    }

    private Object resolveLocalBean(String name, Class<?> type, boolean required, String fieldName) {
        Object bean;
        if (!name.isEmpty()) {
            bean = registry.getByName(name);
            if (bean == null) {
                bean = SharedContext.get(name);
            }
            if (bean != null && !type.isInstance(bean)) {
                throw new BeanCreationException(
                        "Bean '" + name + "' is not of type " + type.getName() + " for field " + fieldName);
            }
        } else {
            bean = registry.getByType(type);
            if (bean == null) {
                bean = SharedContext.getBean(type);
            }
        }
        if (bean == null && required) {
            throw new BeanNotFoundException(fieldName, type);
        }
        return bean;
    }

    private Object resolveGlobalBean(String name, Class<?> type, boolean required, String fieldName) {
        Object bean;
        if (!name.isEmpty()) {
            bean = SharedContext.get(name);
            if (bean != null && !type.isInstance(bean)) {
                throw new BeanCreationException(
                        "Global bean '" + name + "' is not of type " + type.getName() + " for field " + fieldName);
            }
        } else {
            bean = SharedContext.getBean(type);
        }
        if (bean == null && required) {
            throw new BeanNotFoundException(fieldName, type);
        }
        return bean;
    }

    private void invokeBeanFactories() {
        for (BeanDefinition def : definitions) {
            if (!def.isConfiguration()) continue;
            Object configInstance = registry.getByName(def.getBeanName());
            if (configInstance == null) continue;
            for (Method method : def.getBeanMethods()) {
                try {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] params = resolveParams(paramTypes,
                            method.getParameterAnnotations(), def.getBeanClass().getName());
                    Object result = method.invoke(configInstance, params);
                    Bean beanAnn = method.getAnnotation(Bean.class);
                    String name = beanAnn.name().isEmpty() ? method.getName() : beanAnn.name();
                    registry.register(name, result.getClass(), result);
                } catch (Exception e) {
                    throw new BeanCreationException(
                            "Failed to invoke @Bean method " + method.getName()
                                    + " on " + def.getBeanClass().getName(), e);
                }
            }
        }
    }

    private void invokePostConstructs() {
        for (BeanDefinition def : definitions) {
            if (def.isPrototype()) continue;
            Object instance = registry.getByName(def.getBeanName());
            if (instance == null) continue;
            lifecycleProcessor.invokePostConstruct(instance, def.getPostConstructMethods());
        }
    }

    private void registerGlobalBeans() {
        for (BeanDefinition def : definitions) {
            if (!def.isGlobal()) continue;
            Object instance = registry.getByName(def.getBeanName());
            if (instance != null) {
                SharedContext.register(def.getBeanClass(), instance);
            }
        }
    }

    public <T> T getBean(Class<T> type) {
        return registry.getByType(type);
    }

    public Object getBean(String name) {
        return registry.getByName(name);
    }

    public <T> List<T> getBeans(Class<T> type) {
        return registry.getAllByType(type);
    }

    public ApplicationContext registerSingleton(Class<?> type, Object instance) {
        String name = decapitalize(type.getSimpleName());
        registry.register(name, type, instance);
        return this;
    }

    public ApplicationContext registerSingleton(String name, Class<?> type, Object instance) {
        registry.register(name, type, instance);
        return this;
    }

    public Map<String, Object> getBeansByName() {
        return registry.getBeansByName();
    }

    public Map<Class<?>, Object> getBeansByType() {
        return registry.getBeansByType();
    }

    public PropertyLoader getPropertyLoader() {
        return propertyLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void shutdown() {
        List<BeanDefinition> reversed = new ArrayList<>(definitions);
        Collections.reverse(reversed);
        for (BeanDefinition def : reversed) {
            if (def.isPrototype()) continue;
            Object instance = registry.getByName(def.getBeanName());
            if (instance == null) continue;
            lifecycleProcessor.invokePreDestroy(instance, def.getPreDestroyMethods());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertValue(String value, Class<T> targetType) {
        if (targetType == String.class) return (T) value;
        if (targetType == int.class || targetType == Integer.class) return (T) Integer.valueOf(value);
        if (targetType == long.class || targetType == Long.class) return (T) Long.valueOf(value);
        if (targetType == boolean.class || targetType == Boolean.class) return (T) Boolean.valueOf(value);
        if (targetType == double.class || targetType == Double.class) return (T) Double.valueOf(value);
        if (targetType == float.class || targetType == Float.class) return (T) Float.valueOf(value);
        if (targetType == short.class || targetType == Short.class) return (T) Short.valueOf(value);
        if (targetType == byte.class || targetType == Byte.class) return (T) Byte.valueOf(value);
        return (T) value;
    }

    private static String decapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        char[] chars = str.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}
