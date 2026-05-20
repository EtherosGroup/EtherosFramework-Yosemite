package cn.skilfully.etherosframework.di.exception;

public class BeanNotFoundException extends RuntimeException {

    public BeanNotFoundException(String message) {
        super(message);
    }

    public BeanNotFoundException(Class<?> type) {
        super("No bean found for type: " + type.getName());
    }

    public BeanNotFoundException(String name, Class<?> type) {
        super("No bean found for name '" + name + "' of type: " + type.getName());
    }
}
