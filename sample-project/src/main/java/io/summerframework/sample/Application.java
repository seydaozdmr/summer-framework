package io.summerframework.sample;

import io.summerframework.core.context.AnnotationApplicationContext;
import io.summerframework.core.web.ServerTuningProperties;
import io.summerframework.core.web.TinyRestServer;

import java.util.concurrent.CountDownLatch;

public class Application {

    public static void main(String[] args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;

        AnnotationApplicationContext context = new AnnotationApplicationContext(AppConfig.class);
        ServerTuningProperties tuning = ServerTuningProperties.builder()
                .maxConcurrentRequests(1000)
                .coreThreads(8)
                .maxThreads(32)
                .queueCapacity(500)
                .rejectionPolicy(ServerTuningProperties.RejectionPolicy.ABORT)
                .build();

        TinyRestServer server = new TinyRestServer(port, context, tuning);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            context.close();
        }));

        server.start();
        new CountDownLatch(1).await();
    }
}
