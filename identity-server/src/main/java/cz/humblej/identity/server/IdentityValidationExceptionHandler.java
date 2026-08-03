package cz.humblej.identity.server;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class IdentityValidationExceptionHandler {
    private final MessageSource messages;

    IdentityValidationExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidRequest(
            MethodArgumentNotValidException exception, Locale locale) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                messages.getMessage(IdentityServerMessageKeys.INVALID_REQUEST_DETAIL,
                        null, locale));
        problem.setTitle(messages.getMessage(
                IdentityServerMessageKeys.INVALID_REQUEST_TITLE, null, locale));
        problem.setProperty("code", "validation-failed");

        List<Map<String, String>> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "code", error.getCode() == null ? "invalid" : error.getCode()))
                .toList();
        problem.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(problem);
    }
}
