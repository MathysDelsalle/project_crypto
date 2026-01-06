package api.config;

import api.dto.ApiError;
import api.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ===== VALIDATION (@Valid) ===== */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Erreur de validation.");

        HttpStatus status = HttpStatus.BAD_REQUEST;

        // INFO : attendu (erreur utilisateur)
        log.info("Validation error {} {} -> {}",
                request.getMethod(),
                request.getRequestURI(),
                message
        );

        ApiError body = ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /* ===== ERREURS MÉTIER (pas de stacktrace) ===== */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getStatus();

        // WARN : attendu (métier) => pas de stacktrace
        log.warn("Business error {} {} -> {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage()
        );

        ApiError body = ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /* ===== JSON MAL FORMÉ ===== */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Corps de requête invalide (JSON mal formé).";

        log.warn("Bad request body {} {} -> {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage()
        );

        ApiError body = ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /* ===== VRAI BUG (stacktrace) ===== */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // ERROR + stacktrace
        log.error("Unhandled exception {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        ApiError body = ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Erreur interne.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
