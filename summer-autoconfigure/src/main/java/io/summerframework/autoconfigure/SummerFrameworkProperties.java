package io.summerframework.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "summer.framework")
public class SummerFrameworkProperties {

    private boolean enabled = true;
    private boolean includeTimestamp = true;
    private String requestIdHeader = "X-Request-Id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    public String getRequestIdHeader() {
        return requestIdHeader;
    }

    public void setRequestIdHeader(String requestIdHeader) {
        this.requestIdHeader = requestIdHeader;
    }
}
