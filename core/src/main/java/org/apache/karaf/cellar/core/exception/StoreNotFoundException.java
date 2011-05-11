package org.apache.karaf.cellar.core.exception;

/**
 * @author: iocanel
 */
public class StoreNotFoundException extends Exception {

    public StoreNotFoundException() {
    }

    public StoreNotFoundException(String message) {
        super(message);
    }

    public StoreNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public StoreNotFoundException(Throwable cause) {
        super(cause);
    }
}
