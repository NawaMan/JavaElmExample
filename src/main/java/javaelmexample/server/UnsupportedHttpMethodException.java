package javaelmexample.server;

/**
 * This exception indicates that the a HTTP method is not found.
 */
public class UnsupportedHttpMethodException extends UnsupportedOperationException {
    
    private static final long serialVersionUID = -7274047412248465985L;
    
    public UnsupportedHttpMethodException() {
    }
    
    public UnsupportedHttpMethodException(String message) {
        super(message);
    }
    
    public UnsupportedHttpMethodException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UnsupportedHttpMethodException(Throwable cause) {
        super(cause);
    }
    
}
