package io.summerframework.sample;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello from Summer Framework");
    }

    @PostMapping("/echo")
    public Map<String, String> echo(@Valid @RequestBody EchoRequest request) {
        return Map.of("echo", request.text());
    }

    @GetMapping("/fail")
    public Map<String, String> fail() {
        throw new IllegalStateException("Intentional failure");
    }

    public record EchoRequest(@NotBlank String text) {
    }
}
