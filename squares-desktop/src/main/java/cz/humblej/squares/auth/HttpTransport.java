package cz.humblej.squares.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class HttpTransport {
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;

    Response get(URI uri, String bearerToken) throws IOException {
        HttpURLConnection connection = open(uri, "GET");
        if (bearerToken != null) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        return execute(connection, null);
    }

    Response postForm(URI uri, String form) throws IOException {
        HttpURLConnection connection = open(uri, "POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        return execute(connection, form.getBytes(StandardCharsets.UTF_8));
    }

    Response putJson(URI uri, String json, String bearerToken) throws IOException {
        HttpURLConnection connection = open(uri, "PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json, application/problem+json");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        return execute(connection, json.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpURLConnection open(URI uri, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json, application/problem+json");
        return connection;
    }

    private static Response execute(HttpURLConnection connection, byte[] body) throws IOException {
        try {
            if (body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] responseBody = stream == null ? new byte[0] : readAll(stream);
            return new Response(status, responseBody, connection.getHeaderFields());
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    static final class Response {
        private final int status;
        private final byte[] body;
        private final Map<String, List<String>> headers;

        Response(int status, byte[] body, Map<String, List<String>> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers == null ? Collections.<String, List<String>>emptyMap() : headers;
        }

        int status() {
            return status;
        }

        byte[] body() {
            return body;
        }

        Map<String, List<String>> headers() {
            return headers;
        }
    }
}
