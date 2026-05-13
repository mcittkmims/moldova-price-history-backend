package md.pricehistory.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(exception.statusCode())
                .body(error(exception.statusCode().value(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity
                .badRequest()
                .body(error(HttpStatus.BAD_REQUEST.value(), badRequestMessage(exception), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(error(
                        HttpStatus.BAD_REQUEST.value(),
                        validationMessage(exception),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleHandlerValidationException(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(error(
                        HttpStatus.BAD_REQUEST.value(),
                        validationMessage(exception),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity
                .badRequest()
                .body(error(HttpStatus.BAD_REQUEST.value(), "Malformed or missing request body", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(error(HttpStatus.METHOD_NOT_ALLOWED.value(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Content-Type must be application/json", request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND.value(), "No route found for " + request.getRequestURI(), request.getRequestURI()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<ApiError> handleAuthorizationDenied(AuthorizationDeniedException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN.value(), "Access Denied: You don't have permission to perform this action", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", request.getRequestURI()));
    }

    private ApiError error(int status, String message, String path) {
        return new ApiError(Instant.now(), status, HttpStatus.valueOf(status).getReasonPhrase(), message, path);
    }

    private String badRequestMessage(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException missingParameterException) {
            return "Missing required parameter: " + missingParameterException.getParameterName();
        }

        return exception.getMessage();
    }

    private String validationMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage();
        }

        return exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("Validation failed");
    }

    private String validationMessage(HandlerMethodValidationException exception) {
        return exception.getParameterValidationResults()
                .stream()
                .flatMap((result) -> result.getResolvableErrors().stream())
                .map((resolvable) -> resolvable.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("Validation failed");
    }
}
