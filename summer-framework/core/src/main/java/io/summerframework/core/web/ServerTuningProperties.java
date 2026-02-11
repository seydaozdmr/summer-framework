package io.summerframework.core.web;

public final class ServerTuningProperties {

    public enum RejectionPolicy {
        ABORT,
        CALLER_RUNS,
        DISCARD_OLDEST
    }

    private final int coreThreads;
    private final int maxThreads;
    private final int queueCapacity;
    private final int keepAliveSeconds;
    private final int maxConcurrentRequests;
    private final long requestTimeoutMillis;
    private final int socketBacklog;
    private final RejectionPolicy rejectionPolicy;

    private ServerTuningProperties(Builder builder) {
        this.coreThreads = builder.coreThreads;
        this.maxThreads = builder.maxThreads;
        this.queueCapacity = builder.queueCapacity;
        this.keepAliveSeconds = builder.keepAliveSeconds;
        this.maxConcurrentRequests = builder.maxConcurrentRequests;
        this.requestTimeoutMillis = builder.requestTimeoutMillis;
        this.socketBacklog = builder.socketBacklog;
        this.rejectionPolicy = builder.rejectionPolicy;
        validate();
    }

    public static Builder builder() {
        int processors = Runtime.getRuntime().availableProcessors();
        int defaultCore = Math.max(2, processors);
        int defaultMax = Math.max(defaultCore, processors * 2);
        return new Builder()
                .coreThreads(defaultCore)
                .maxThreads(defaultMax)
                .queueCapacity(256)
                .keepAliveSeconds(30)
                .maxConcurrentRequests(512)
                .requestTimeoutMillis(0)
                .socketBacklog(1024)
                .rejectionPolicy(RejectionPolicy.CALLER_RUNS);
    }

    public int coreThreads() {
        return coreThreads;
    }

    public int maxThreads() {
        return maxThreads;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public int keepAliveSeconds() {
        return keepAliveSeconds;
    }

    public int maxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public long requestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public int socketBacklog() {
        return socketBacklog;
    }

    public RejectionPolicy rejectionPolicy() {
        return rejectionPolicy;
    }

    private void validate() {
        if (coreThreads <= 0) {
            throw new IllegalArgumentException("coreThreads must be > 0");
        }
        if (maxThreads < coreThreads) {
            throw new IllegalArgumentException("maxThreads must be >= coreThreads");
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must be >= 0");
        }
        if (keepAliveSeconds < 0) {
            throw new IllegalArgumentException("keepAliveSeconds must be >= 0");
        }
        if (maxConcurrentRequests <= 0) {
            throw new IllegalArgumentException("maxConcurrentRequests must be > 0");
        }
        if (requestTimeoutMillis < 0) {
            throw new IllegalArgumentException("requestTimeoutMillis must be >= 0");
        }
        if (socketBacklog <= 0) {
            throw new IllegalArgumentException("socketBacklog must be > 0");
        }
    }

    public static final class Builder {

        private int coreThreads;
        private int maxThreads;
        private int queueCapacity;
        private int keepAliveSeconds;
        private int maxConcurrentRequests;
        private long requestTimeoutMillis;
        private int socketBacklog;
        private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;

        public Builder coreThreads(int coreThreads) {
            this.coreThreads = coreThreads;
            return this;
        }

        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder keepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
            return this;
        }

        public Builder maxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
            return this;
        }

        public Builder requestTimeoutMillis(long requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
            return this;
        }

        public Builder socketBacklog(int socketBacklog) {
            this.socketBacklog = socketBacklog;
            return this;
        }

        public Builder rejectionPolicy(RejectionPolicy rejectionPolicy) {
            this.rejectionPolicy = rejectionPolicy;
            return this;
        }

        public ServerTuningProperties build() {
            return new ServerTuningProperties(this);
        }
    }
}
