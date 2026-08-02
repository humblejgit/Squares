package cz.humblej.identity.server.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
class ApiSecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", "A valid bearer token is required.", "unauthorized");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", "The operation is not permitted.", "forbidden");
    }

    private static void write(
            HttpServletResponse response,
            int status,
            String title,
            String detail,
            String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s"}
                """.formatted(title, status, detail, code));
    }
}
