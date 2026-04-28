package md.pricehistory.backend.common;

import org.springframework.http.HttpStatusCode;

public class ApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public ApiException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }
}
