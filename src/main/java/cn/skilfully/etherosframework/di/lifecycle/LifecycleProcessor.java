package cn.skilfully.etherosframework.di.lifecycle;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LifecycleProcessor {

    private static final Logger LOG = Logger.getLogger("EtherosFramework-Lifecycle");

    public void invokePostConstruct(Object bean, List<Method> methods) {
        for (Method method : methods) {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Failed to invoke @PostConstruct on " + bean.getClass().getName() + "." + method.getName(), e);
            }
        }
    }

    public void invokePreDestroy(Object bean, List<Method> methods) {
        for (Method method : methods) {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Failed to invoke @PreDestroy on " + bean.getClass().getName() + "." + method.getName(), e);
            }
        }
    }
}
