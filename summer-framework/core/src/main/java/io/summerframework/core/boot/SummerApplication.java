package io.summerframework.core.boot;

import io.summerframework.core.context.AnnotationApplicationContext;
import io.summerframework.core.web.ServerTuningProperties;
import io.summerframework.core.web.TinyRestServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public final class SummerApplication {

    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";

    private SummerApplication() {
    }

    public static void run(Class<?> configurationClass, String... args) throws InterruptedException {
        start(configurationClass, args);
        new CountDownLatch(1).await();
    }

    public static RunningApplication start(Class<?> configurationClass, String... args) {
        Objects.requireNonNull(configurationClass, "configurationClass must not be null");

        Properties properties = loadClasspathProperties(DEFAULT_PROPERTIES_FILE);
        applyCommandLineOverrides(properties, args);

        int port = readInt(properties, "server.port", 8080);
        ServerTuningProperties tuning = resolveTuning(properties);

        AnnotationApplicationContext context = new AnnotationApplicationContext(configurationClass);
        TinyRestServer server = new TinyRestServer(port, context, tuning);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            context.close();
        }));

        server.start();
        return new RunningApplication(server, context);
    }

    private static Properties loadClasspathProperties(String fileName) {
        Properties properties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = SummerApplication.class.getClassLoader();
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load " + fileName, ex);
        }

        return properties;
    }

    private static void applyCommandLineOverrides(Properties properties, String[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        boolean hasNamedArgs = false;
        for (String arg : args) {
            if (arg != null && arg.startsWith("--")) {
                hasNamedArgs = true;
                break;
            }
        }

        if (hasNamedArgs) {
            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                if (!arg.startsWith("--")) {
                    throw new IllegalArgumentException("Invalid argument: " + arg + ". Use --key=value format.");
                }

                String option = arg.substring(2);
                int separator = option.indexOf('=');
                if (separator <= 0 || separator == option.length() - 1) {
                    throw new IllegalArgumentException("Invalid argument: " + arg + ". Use --key=value format.");
                }

                String key = option.substring(0, separator).trim();
                String value = option.substring(separator + 1).trim();
                properties.setProperty(key, value);
            }
            return;
        }

        String[] keys = {
                "server.port",
                "summer.server.request-timeout-millis",
                "summer.server.max-concurrent-requests",
                "summer.server.core-threads",
                "summer.server.max-threads",
                "summer.server.queue-capacity",
                "summer.server.rejection-policy",
                "summer.server.socket-backlog"
        };

        int max = Math.min(args.length, keys.length);
        for (int i = 0; i < max; i++) {
            String value = args[i];
            if (value == null || value.isBlank()) {
                continue;
            }
            properties.setProperty(keys[i], value.trim());
        }
    }

    private static ServerTuningProperties resolveTuning(Properties properties) {
        ServerTuningProperties.Builder builder = ServerTuningProperties.builder();

        Integer coreThreads = readOptionalInt(properties, "summer.server.core-threads");
        Integer maxThreads = readOptionalInt(properties, "summer.server.max-threads");
        Integer queueCapacity = readOptionalInt(properties, "summer.server.queue-capacity");
        Integer keepAliveSeconds = readOptionalInt(properties, "summer.server.keep-alive-seconds");
        Integer maxConcurrentRequests = readOptionalInt(properties, "summer.server.max-concurrent-requests");
        Long requestTimeoutMillis = readOptionalLong(properties, "summer.server.request-timeout-millis");
        Integer socketBacklog = readOptionalInt(properties, "summer.server.socket-backlog");
        String rejectionPolicy = readOptionalString(properties, "summer.server.rejection-policy");

        if (coreThreads != null) {
            builder.coreThreads(coreThreads);
        }
        if (maxThreads != null) {
            builder.maxThreads(maxThreads);
        }
        if (queueCapacity != null) {
            builder.queueCapacity(queueCapacity);
        }
        if (keepAliveSeconds != null) {
            builder.keepAliveSeconds(keepAliveSeconds);
        }
        if (maxConcurrentRequests != null) {
            builder.maxConcurrentRequests(maxConcurrentRequests);
        }
        if (requestTimeoutMillis != null) {
            builder.requestTimeoutMillis(requestTimeoutMillis);
        }
        if (socketBacklog != null) {
            builder.socketBacklog(socketBacklog);
        }
        if (rejectionPolicy != null) {
            builder.rejectionPolicy(ServerTuningProperties.RejectionPolicy.valueOf(rejectionPolicy.toUpperCase(Locale.ROOT)));
        }

        return builder.build();
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer for property '" + key + "': " + value, ex);
        }
    }

    private static Integer readOptionalInt(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer for property '" + key + "': " + value, ex);
        }
    }

    private static Long readOptionalLong(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid long for property '" + key + "': " + value, ex);
        }
    }

    private static String readOptionalString(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record RunningApplication(TinyRestServer server, AnnotationApplicationContext context) implements AutoCloseable {
        @Override
        public void close() {
            server.stop();
            context.close();
        }
    }
}
