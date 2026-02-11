package io.summerframework.core.web;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class TunedExecutorFactory {

    ThreadPoolExecutor create(ServerTuningProperties properties, String prefix) {
        BlockingQueue<Runnable> queue = properties.queueCapacity() == 0
                ? new SynchronousQueue<>()
                : new ArrayBlockingQueue<>(properties.queueCapacity());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                properties.coreThreads(),
                properties.maxThreads(),
                properties.keepAliveSeconds(),
                TimeUnit.SECONDS,
                queue,
                namedThreadFactory(prefix),
                rejectionHandler(properties.rejectionPolicy()));

        executor.allowCoreThreadTimeOut(properties.keepAliveSeconds() > 0);
        return executor;
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
    }

    private RejectedExecutionHandler rejectionHandler(ServerTuningProperties.RejectionPolicy rejectionPolicy) {
        return switch (rejectionPolicy) {
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS -> new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD_OLDEST -> new ThreadPoolExecutor.DiscardOldestPolicy();
        };
    }
}
