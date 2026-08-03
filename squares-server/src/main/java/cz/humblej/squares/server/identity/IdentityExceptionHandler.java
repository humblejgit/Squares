package cz.humblej.squares.server.identity;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cz.humblej.squares.server.ServerMessageKeys;

@RestControllerAdvice
class IdentityExceptionHandler {
    private final MessageSource messages;

    IdentityExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(HandleUnavailableException.class)
    ResponseEntity<ProblemDetail> handleUnavailable(
            HandleUnavailableException exception, Locale locale) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                messages.getMessage(ServerMessageKeys.HANDLE_UNAVAILABLE_DETAIL, null, locale));
        problem.setTitle(messages.getMessage(
                ServerMessageKeys.HANDLE_UNAVAILABLE_TITLE, null, locale));
        problem.setProperty("code", "handle-unavailable");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
