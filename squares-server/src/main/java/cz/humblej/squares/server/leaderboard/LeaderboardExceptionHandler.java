package cz.humblej.squares.server.leaderboard;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cz.humblej.squares.server.ServerMessageKeys;

@RestControllerAdvice(assignableTypes = LeaderboardController.class)
class LeaderboardExceptionHandler {
    private final MessageSource messages;

    LeaderboardExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(InvalidLeaderboardCursorException.class)
    ResponseEntity<ProblemDetail> invalidCursor(
            InvalidLeaderboardCursorException exception, Locale locale) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                messages.getMessage(
                        ServerMessageKeys.LEADERBOARD_INVALID_CURSOR_DETAIL, null, locale));
        problem.setTitle(messages.getMessage(
                ServerMessageKeys.LEADERBOARD_INVALID_CURSOR_TITLE, null, locale));
        problem.setProperty("code", "invalid-leaderboard-cursor");
        return ResponseEntity.badRequest().body(problem);
    }
}
