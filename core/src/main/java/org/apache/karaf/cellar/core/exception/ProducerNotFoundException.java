package org.apache.karaf.cellar.core.exception;

/**
 * @author: iocanel
 */
public class ProducerNotFoundException extends Exception {

    public ProducerNotFoundException() {
    }

    public ProducerNotFoundException(String message) {
        super(message);
    }

    public ProducerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProducerNotFoundException(Throwable cause) {
        super(cause);
    }
}
