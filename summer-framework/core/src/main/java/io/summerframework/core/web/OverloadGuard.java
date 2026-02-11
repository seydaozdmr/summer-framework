package io.summerframework.core.web;

import java.util.concurrent.Semaphore;

final class OverloadGuard {

    private final Semaphore semaphore;

    OverloadGuard(int maxConcurrentRequests) {
        this.semaphore = new Semaphore(maxConcurrentRequests);
    }

    boolean tryEnter() {
        return semaphore.tryAcquire();
    }

    void exit() {
        semaphore.release();
    }
}
