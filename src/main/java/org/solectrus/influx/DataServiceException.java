package org.solectrus.influx;

public class DataServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public DataServiceException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
