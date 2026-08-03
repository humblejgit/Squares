package cz.humblej.squares.server.submission;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cz.humblej.squares.server.ServerMessageKeys;

@RestControllerAdvice
class SubmissionExceptionHandler {
    private final MessageSource messages;

    SubmissionExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(SubmissionPayloadConflictException.class)
    ResponseEntity<ProblemDetail> payloadConflict(
            SubmissionPayloadConflictException exception, Locale locale) {
        return problem(HttpStatus.CONFLICT,
                ServerMessageKeys.SUBMISSION_CONFLICT_TITLE,
                ServerMessageKeys.SUBMISSION_CONFLICT_DETAIL,
                "submission-payload-conflict", locale);
    }

    @ExceptionHandler(InvalidSubmissionException.class)
    ResponseEntity<ProblemDetail> invalidSubmission(
            InvalidSubmissionException exception, Locale locale) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY,
                ServerMessageKeys.INVALID_SUBMISSION_TITLE,
                exception.detailKey(), "invalid-game-submission", locale);
    }

    @ExceptionHandler(InstallationNotRegisteredException.class)
    ResponseEntity<ProblemDetail> installationNotRegistered(
            InstallationNotRegisteredException exception, Locale locale) {
        return problem(HttpStatus.FORBIDDEN,
                ServerMessageKeys.INSTALLATION_NOT_REGISTERED_TITLE,
                ServerMessageKeys.INSTALLATION_NOT_REGISTERED_DETAIL,
                "installation-not-registered", locale);
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(
            SubmissionNotFoundException exception, Locale locale) {
        return problem(HttpStatus.NOT_FOUND,
                ServerMessageKeys.SUBMISSION_NOT_FOUND_TITLE,
                ServerMessageKeys.SUBMISSION_NOT_FOUND_DETAIL,
                "game-submission-not-found", locale);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String titleKey, String detailKey,
            String code, Locale locale) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, messages.getMessage(detailKey, null, locale));
        problem.setTitle(messages.getMessage(titleKey, null, locale));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
