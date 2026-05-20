package cn.skilfully.etherosframework.hook.exception;

public class HookException extends RuntimeException {

    public HookException(String message) {
        super(message);
    }

    public HookException(String message, Throwable cause) {
        super(message, cause);
    }
}
