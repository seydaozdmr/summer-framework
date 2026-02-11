package io.summerframework.core.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.summerframework.core.context.AnnotationApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TinyRestServer {

    private final int port;
    private final Router router;
    private final Json json;
    private final ServerTuningProperties tuning;
    private final OverloadGuard overloadGuard;
    private final TunedExecutorFactory executorFactory;
    private ThreadPoolExecutor ioExecutor;
    private ThreadPoolExecutor invocationExecutor;
    private HttpServer httpServer;

    public TinyRestServer(int port, AnnotationApplicationContext context) {
        this(port, context, ServerTuningProperties.builder().build());
    }

    public TinyRestServer(int port, AnnotationApplicationContext context, ServerTuningProperties tuning) {
        this.port = port;
        this.router = Router.fromContext(context);
        this.json = new Json();
        this.tuning = tuning;
        this.overloadGuard = new OverloadGuard(tuning.maxConcurrentRequests());
        this.executorFactory = new TunedExecutorFactory();
    }

    public void start() {
        if (httpServer != null) {
            throw new IllegalStateException("Server already started");
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), tuning.socketBacklog());
            ioExecutor = executorFactory.create(tuning, "summer-http");
            httpServer.setExecutor(ioExecutor);
            if (tuning.requestTimeoutMillis() > 0) {
                invocationExecutor = executorFactory.create(tuning, "summer-route");
            }
            httpServer.createContext("/", this::handle);
            httpServer.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start server on port " + port, ex);
        }

        System.out.println("TinyRestServer started at http://localhost:" + port);
        System.out.println("  tuning: coreThreads=" + tuning.coreThreads()
                + ", maxThreads=" + tuning.maxThreads()
                + ", queueCapacity=" + tuning.queueCapacity()
                + ", socketBacklog=" + tuning.socketBacklog()
                + ", maxConcurrentRequests=" + tuning.maxConcurrentRequests()
                + ", requestTimeoutMillis=" + tuning.requestTimeoutMillis()
                + ", rejectionPolicy=" + tuning.rejectionPolicy());
        for (RouteDefinition route : router.getAllRoutes()) {
            System.out.println("  -> " + route.method() + " " + route.path());
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        shutdownExecutor(invocationExecutor);
        invocationExecutor = null;
        shutdownExecutor(ioExecutor);
        ioExecutor = null;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        boolean entered = overloadGuard.tryEnter();
        if (!entered) {
            writeJson(exchange, 503, ApiEnvelope.error("Server is overloaded", 503, path));
            return;
        }

        try {
            RouteMatch routeMatch = router.resolve(exchange.getRequestMethod(), path);
            if (routeMatch == null) {
                writeJson(exchange, 404, ApiEnvelope.error("Route not found", 404, path));
                return;
            }

            String body = readBody(exchange);
            Map<String, List<String>> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
            Map<String, List<String>> headers = parseHeaders(exchange);
            Object result = invokeRoute(routeMatch.route(), body, routeMatch.pathVariables(), queryParams, headers);
            if (result instanceof RestResponse restResponse) {
                int status = restResponse.status();
                if (status == 204) {
                    writeNoContent(exchange);
                    return;
                }
                writeJson(exchange, status, ApiEnvelope.success(restResponse.body(), path));
                return;
            }

            writeJson(exchange, 200, ApiEnvelope.success(result, path));
        } catch (BadRequestException ex) {
            writeJson(exchange, 400, ApiEnvelope.error(ex.getMessage(), 400, path));
        } catch (RequestTimeoutException ex) {
            writeJson(exchange, 504, ApiEnvelope.error(ex.getMessage(), 504, path));
        } catch (RejectedExecutionException ex) {
            writeJson(exchange, 503, ApiEnvelope.error("Server queue is full", 503, path));
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
            writeJson(exchange, 500, ApiEnvelope.error(message, 500, path));
        } finally {
            overloadGuard.exit();
        }
    }

    private Object invokeRoute(RouteDefinition route,
                               String body,
                               Map<String, String> pathVariables,
                               Map<String, List<String>> queryParams,
                               Map<String, List<String>> headers) {
        if (tuning.requestTimeoutMillis() <= 0) {
            return route.invoke(body, pathVariables, queryParams, headers);
        }

        Future<Object> future = invocationExecutor.submit(() -> route.invoke(body, pathVariables, queryParams, headers));
        try {
            return future.get(tuning.requestTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new RequestTimeoutException("Request timed out after " + tuning.requestTimeoutMillis() + " ms");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request processing interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause != null ? cause.getMessage() : "Request processing failed", ex);
        }
    }

    private Map<String, List<String>> parseQueryParams(String rawQuery) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String rawName = separator >= 0 ? pair.substring(0, separator) : pair;
            String rawValue = separator >= 0 ? pair.substring(separator + 1) : "";
            String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            params.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        return params;
    }

    private Map<String, List<String>> parseHeaders(HttpExchange exchange) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            headers.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return headers;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void writeNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Length", "0");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] responseBytes = json.stringify(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private void shutdownExecutor(ThreadPoolExecutor executor) {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
