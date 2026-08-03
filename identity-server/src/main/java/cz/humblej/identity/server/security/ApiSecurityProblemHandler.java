package cz.humblej.identity.server.security;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import cz.humblej.identity.server.IdentityServerMessageKeys;
import tools.jackson.databind.ObjectMapper;

@Component
class ApiSecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("cs-CZ");

    private final MessageSource messages;
    private final ObjectMapper mapper;

    ApiSecurityProblemHandler(MessageSource messages, ObjectMapper mapper) {
        this.messages = messages;
        this.mapper = mapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED,
                IdentityServerMessageKeys.UNAUTHORIZED_TITLE,
                IdentityServerMessageKeys.UNAUTHORIZED_DETAIL,
                "unauthorized", locale(request));
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        write(response, HttpStatus.FORBIDDEN,
                IdentityServerMessageKeys.FORBIDDEN_TITLE,
                IdentityServerMessageKeys.FORBIDDEN_DETAIL,
                "forbidden", locale(request));
    }

    private void write(
            HttpServletResponse response,
            HttpStatus status,
            String titleKey,
            String detailKey,
            String code,
            Locale locale) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, messages.getMessage(detailKey, null, locale));
        problem.setTitle(messages.getMessage(titleKey, null, locale));
        problem.setProperty("code", code);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }

    private static Locale locale(HttpServletRequest request) {
        return request.getHeader("Accept-Language") == null
                ? DEFAULT_LOCALE : request.getLocale();
    }
}
