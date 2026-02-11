package io.summerframework.sample;

import io.summerframework.core.annotation.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class TodoService {

    private final AtomicLong idSequence = new AtomicLong(0);
    private final Map<Long, TodoItem> todos = new ConcurrentHashMap<>();

    public TodoItem create(String title, String note) {
        long id = idSequence.incrementAndGet();
        TodoItem item = new TodoItem(id, title, note, false);
        todos.put(id, item);
        return item;
    }

    public List<TodoItem> list(Boolean completed) {
        List<TodoItem> values = new ArrayList<>(todos.values());
        if (completed != null) {
            values = values.stream()
                    .filter(todo -> todo.completed() == completed)
                    .collect(Collectors.toList());
        }
        values.sort(Comparator.comparingLong(TodoItem::id));
        return values;
    }

    public TodoItem find(long id) {
        return todos.get(id);
    }

    public TodoItem setCompleted(long id, boolean completed) {
        return todos.computeIfPresent(id, (ignored, existing) ->
                new TodoItem(existing.id(), existing.title(), existing.note(), completed));
    }

    public boolean delete(long id) {
        return todos.remove(id) != null;
    }

    public record TodoItem(long id, String title, String note, boolean completed) {
    }
}
