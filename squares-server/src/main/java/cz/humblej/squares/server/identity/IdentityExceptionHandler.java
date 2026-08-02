package cz.humblej.squares.server.identity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class IdentityExceptionHandler {
    @ExceptionHandler(HandleUnavailableException.class)
    ResponseEntity<ProblemDetail> handleUnavailable(HandleUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage());
        problem.setTitle("Handle is unavailable");
        problem.setProperty("code", "handle-unavailable");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
