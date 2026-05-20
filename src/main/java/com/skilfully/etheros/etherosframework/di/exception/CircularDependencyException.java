package com.skilfully.etheros.etherosframework.di.exception;

public class CircularDependencyException extends RuntimeException {

    public CircularDependencyException(String message) {
        super(message);
    }
}
