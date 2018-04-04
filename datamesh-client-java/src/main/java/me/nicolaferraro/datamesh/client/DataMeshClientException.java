package me.nicolaferraro.datamesh.client;

public class DataMeshClientException extends RuntimeException {

    public DataMeshClientException() {
    }

    public DataMeshClientException(String message) {
        super(message);
    }

    public DataMeshClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataMeshClientException(Throwable cause) {
        super(cause);
    }

    public DataMeshClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
