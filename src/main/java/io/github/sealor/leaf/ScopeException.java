package io.github.sealor.leaf;

public class ScopeException extends RuntimeException {
    public ScopeException(String message) {
        super(message);
    }

    public ScopeException(String message, Exception e) {
        super(message, e);
    }

    public ScopeException(Exception e) {
        super(e);
    }

}
