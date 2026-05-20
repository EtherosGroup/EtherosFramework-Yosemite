package cn.skilfully.etherosframework.di.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BeanRegistry {
    private final Map<String, Object> beansByName = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> beansByType = new ConcurrentHashMap<>();

    public void register(String name, Class<?> type, Object instance) {
        beansByName.put(name, instance);
        beansByType.put(type, instance);
    }

    public Object getByName(String name) {
        return beansByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getByType(Class<T> type) {
        Object bean = beansByType.get(type);
        if (bean != null) {
            return (T) bean;
        }
        for (Map.Entry<Class<?>, Object> entry : beansByType.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return (T) entry.getValue();
            }
        }
        return null;
    }

    public <T> List<T> getAllByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Map.Entry<Class<?>, Object> entry : beansByType.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                result.add(type.cast(entry.getValue()));
            }
        }
        return result;
    }

    public boolean containsType(Class<?> type) {
        if (beansByType.containsKey(type)) return true;
        for (Class<?> key : beansByType.keySet()) {
            if (type.isAssignableFrom(key)) return true;
        }
        return false;
    }

    public Map<String, Object> getBeansByName() {
        return Collections.unmodifiableMap(beansByName);
    }

    public Map<Class<?>, Object> getBeansByType() {
        return Collections.unmodifiableMap(beansByType);
    }

    public void clear() {
        beansByName.clear();
        beansByType.clear();
    }
}
