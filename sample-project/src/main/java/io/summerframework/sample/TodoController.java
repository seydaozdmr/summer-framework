package io.summerframework.sample;

import io.summerframework.core.web.RestResponse;
import io.summerframework.core.web.annotation.DeleteMapping;
import io.summerframework.core.web.annotation.GetMapping;
import io.summerframework.core.web.annotation.PatchMapping;
import io.summerframework.core.web.annotation.PathVariable;
import io.summerframework.core.web.annotation.PostMapping;
import io.summerframework.core.web.annotation.RequestBody;
import io.summerframework.core.web.annotation.RequestMapping;
import io.summerframework.core.web.annotation.RequestParam;
import io.summerframework.core.web.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "todo-sample-app",
                "status", "UP");
    }

    @PostMapping("/todos")
    public RestResponse create(@RequestBody CreateTodoRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            return new RestResponse(400, Map.of("message", "title is required"));
        }

        TodoService.TodoItem created = todoService.create(request.title(), request.note());
        return RestResponse.created(created);
    }

    @GetMapping("/todos")
    public Map<String, Object> list(
            @RequestParam(value = "completed", required = false) Boolean completed) {
        var items = todoService.list(completed);
        return Map.of(
                "count", items.size(),
                "items", items);
    }

    @GetMapping("/todos/{id}")
    public RestResponse getById(@PathVariable("id") long id) {
        TodoService.TodoItem item = todoService.find(id);
        if (item == null) {
            return new RestResponse(404, Map.of("message", "todo not found", "id", id));
        }
        return RestResponse.ok(item);
    }

    @PatchMapping("/todos/{id}/completed")
    public RestResponse setCompleted(
            @PathVariable("id") long id,
            @RequestBody UpdateCompletedRequest request) {
        TodoService.TodoItem updated = todoService.setCompleted(id, request.completed());
        if (updated == null) {
            return new RestResponse(404, Map.of("message", "todo not found", "id", id));
        }
        return RestResponse.ok(updated);
    }

    @DeleteMapping("/todos/{id}")
    public RestResponse delete(@PathVariable("id") long id) {
        boolean deleted = todoService.delete(id);
        if (!deleted) {
            return new RestResponse(404, Map.of("message", "todo not found", "id", id));
        }
        return RestResponse.noContent();
    }

    public record CreateTodoRequest(String title, String note) {
    }

    public record UpdateCompletedRequest(boolean completed) {
    }
}
