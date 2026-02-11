package io.summerframework.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.summerframework.autoconfigure.web.RequestIdFilter;
import io.summerframework.autoconfigure.web.SummerExceptionHandler;
import io.summerframework.autoconfigure.web.SummerResponseAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({DispatcherServlet.class, ResponseBodyAdvice.class, RequestContextHolder.class})
@ConditionalOnProperty(prefix = "summer.framework", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SummerFrameworkProperties.class)
public class SummerFrameworkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RequestIdFilter.class)
    public RequestIdFilter requestIdFilter(SummerFrameworkProperties properties) {
        return new RequestIdFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(SummerResponseAdvice.class)
    public SummerResponseAdvice summerResponseAdvice(SummerFrameworkProperties properties, ObjectMapper objectMapper) {
        return new SummerResponseAdvice(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SummerExceptionHandler.class)
    public SummerExceptionHandler summerExceptionHandler(SummerFrameworkProperties properties) {
        return new SummerExceptionHandler(properties);
    }
}
