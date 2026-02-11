package io.summerframework.example;

import io.summerframework.core.web.annotation.GetMapping;
import io.summerframework.core.web.annotation.DeleteMapping;
import io.summerframework.core.web.annotation.PathVariable;
import io.summerframework.core.web.annotation.PatchMapping;
import io.summerframework.core.web.annotation.PostMapping;
import io.summerframework.core.web.annotation.PutMapping;
import io.summerframework.core.web.annotation.RequestBody;
import io.summerframework.core.web.annotation.RequestHeader;
import io.summerframework.core.web.annotation.RequestMapping;
import io.summerframework.core.web.annotation.RequestParam;
import io.summerframework.core.web.annotation.RestController;
import io.summerframework.core.web.RestResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserFacade userFacade;
    private final ClockService clockService;

    public UserController(UserFacade userFacade, ClockService clockService) {
        this.userFacade = userFacade;
        this.clockService = clockService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "summer-framework",
                "status", "UP",
                "time", clockService.now().toString());
    }

    @PostMapping("/welcome")
    public Map<String, Object> welcome(@RequestBody WelcomeRequest request) {
        String message = userFacade.welcome(request.name());
        return Map.of("message", message, "name", request.name());
    }

    @GetMapping("/slow")
    public Map<String, Object> slow() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Slow request interrupted", ex);
        }
        return Map.of("result", "slow-ok");
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable("id") long id,
                                       @RequestParam(value = "verbose", required = false, defaultValue = "false") boolean verbose) {
        return Map.of(
                "id", id,
                "name", "user-" + id,
                "verbose", verbose);
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable("id") long id,
                                          @RequestBody UpdateUserRequest request) {
        return Map.of(
                "id", id,
                "updatedName", request.name(),
                "active", request.active());
    }

    @DeleteMapping("/users/{id}")
    public RestResponse deleteUser(@PathVariable("id") long id) {
        return RestResponse.noContent();
    }

    @PatchMapping("/users/{id}/status")
    public Map<String, Object> patchUserStatus(@PathVariable("id") long id,
                                               @RequestBody PatchStatusRequest request,
                                               @RequestHeader(value = "x-actor", required = false, defaultValue = "system") String actor) {
        return Map.of(
                "id", id,
                "active", request.active(),
                "actor", actor,
                "patched", true);
    }

    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam("tag") List<String> tags,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            @RequestHeader(value = "x-request-id", required = false, defaultValue = "n/a") String requestId) {
        return Map.of(
                "tags", tags,
                "limit", limit,
                "requestId", requestId);
    }

    public record WelcomeRequest(String name) {
    }

    public record UpdateUserRequest(String name, boolean active) {
    }

    public record PatchStatusRequest(boolean active) {
    }
}
