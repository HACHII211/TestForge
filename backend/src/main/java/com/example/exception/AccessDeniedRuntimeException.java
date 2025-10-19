// AccessDeniedRuntimeException.java
package com.example.exception;

public class AccessDeniedRuntimeException extends RuntimeException {

    public AccessDeniedRuntimeException() {
        super("Access Denied: insufficient permissions");
    }

    public AccessDeniedRuntimeException(String message) {
        super(message);
    }

    public AccessDeniedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
