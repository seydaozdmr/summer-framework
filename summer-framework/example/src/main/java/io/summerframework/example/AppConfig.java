package io.summerframework.example;

import io.summerframework.core.annotation.Bean;
import io.summerframework.core.annotation.ComponentScan;
import io.summerframework.core.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "io.summerframework.example")
public class AppConfig {

    @Bean
    public ClockService clockService() {
        return new ClockService();
    }
}
