package cn.skilfully.etherosframework.di.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SharedContext {

    private static final Map<String, Object> beansByName = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Object>> beansByType = new ConcurrentHashMap<>();

    private SharedContext() {
    }

    public static synchronized void register(Class<?> type, Object bean) {
        beansByName.put(type.getSimpleName(), bean);
        beansByType.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(bean);
    }

    public static synchronized void syncFrom(ApplicationContext context) {
        for (Map.Entry<String, Object> entry : context.getBeansByName().entrySet()) {
            beansByName.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Class<?>, Object> entry : context.getBeansByType().entrySet()) {
            beansByType.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArrayList<>())
                    .add(entry.getValue());
        }
    }

    public static Object get(String name) {
        return beansByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> type) {
        List<Object> list = beansByType.get(type);
        if (list != null && !list.isEmpty()) {
            return (T) list.get(0);
        }
        for (Map.Entry<Class<?>, List<Object>> entry : beansByType.entrySet()) {
            if (type.isAssignableFrom(entry.getKey()) && !entry.getValue().isEmpty()) {
                return (T) entry.getValue().get(0);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getBeans(Class<T> type) {
        List<Object> list = beansByType.get(type);
        if (list != null) {
            return (List<T>) Collections.unmodifiableList(list);
        }
        List<T> result = new ArrayList<>();
        for (Map.Entry<Class<?>, List<Object>> entry : beansByType.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                for (Object bean : entry.getValue()) {
                    result.add((T) bean);
                }
            }
        }
        return result;
    }

    public static boolean contains(Class<?> type) {
        if (beansByType.containsKey(type) && !beansByType.get(type).isEmpty()) return true;
        for (Map.Entry<Class<?>, List<Object>> entry : beansByType.entrySet()) {
            if (type.isAssignableFrom(entry.getKey()) && !entry.getValue().isEmpty()) return true;
        }
        return false;
    }

    public static void clear() {
        beansByName.clear();
        beansByType.clear();
    }
}
