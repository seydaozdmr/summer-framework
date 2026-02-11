package io.summerframework.example;

import io.summerframework.core.annotation.Autowired;
import io.summerframework.core.annotation.Component;

@Component
public class UserFacade {

    private final GreetingService greetingService;

    @Autowired
    private ClockService clockService;

    public UserFacade(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    public String welcome(String name) {
        return greetingService.greet(name) + " at " + clockService.now();
    }
}
