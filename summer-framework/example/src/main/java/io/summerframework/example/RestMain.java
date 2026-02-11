package io.summerframework.example;

import io.summerframework.core.context.AnnotationApplicationContext;
import io.summerframework.core.web.ServerTuningProperties;
import io.summerframework.core.web.TinyRestServer;

import java.util.concurrent.CountDownLatch;

public class RestMain {

    public static void main(String[] args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        long timeoutMillis = args.length > 1 ? Long.parseLong(args[1]) : 0L;
        int maxConcurrentRequests = args.length > 2 ? Integer.parseInt(args[2]) : 512;
        Integer coreThreads = args.length > 3 ? Integer.parseInt(args[3]) : null;
        Integer maxThreads = args.length > 4 ? Integer.parseInt(args[4]) : null;
        Integer queueCapacity = args.length > 5 ? Integer.parseInt(args[5]) : null;
        ServerTuningProperties.RejectionPolicy rejectionPolicy = args.length > 6
                ? ServerTuningProperties.RejectionPolicy.valueOf(args[6].toUpperCase())
                : null;
        Integer socketBacklog = args.length > 7 ? Integer.parseInt(args[7]) : null;

        AnnotationApplicationContext context = new AnnotationApplicationContext(AppConfig.class);
        ServerTuningProperties.Builder tuningBuilder = ServerTuningProperties.builder()
                .requestTimeoutMillis(timeoutMillis)
                .maxConcurrentRequests(maxConcurrentRequests);

        if (coreThreads != null) {
            tuningBuilder.coreThreads(coreThreads);
        }
        if (maxThreads != null) {
            tuningBuilder.maxThreads(maxThreads);
        }
        if (queueCapacity != null) {
            tuningBuilder.queueCapacity(queueCapacity);
        }
        if (rejectionPolicy != null) {
            tuningBuilder.rejectionPolicy(rejectionPolicy);
        }
        if (socketBacklog != null) {
            tuningBuilder.socketBacklog(socketBacklog);
        }

        ServerTuningProperties tuning = tuningBuilder.build();
        TinyRestServer server = new TinyRestServer(port, context, tuning);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            context.close();
        }));

        server.start();
        new CountDownLatch(1).await();
    }
}
