package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.UpdateTodoRequest;
import com.example.todo.model.TodoItem;
import com.example.todo.repository.DynamoDbTodoRepository;
import com.example.todo.repository.InMemoryTodoRepository;
import com.example.todo.repository.TodoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);

    private final DynamoDbTodoRepository dynamoDbRepo;
    private final InMemoryTodoRepository inMemoryRepo;

    @Autowired
    public TodoService(DynamoDbTodoRepository dynamoDbRepo, InMemoryTodoRepository inMemoryRepo) {
        this.dynamoDbRepo = dynamoDbRepo;
        this.inMemoryRepo = inMemoryRepo;
    }

    private TodoRepository getActiveRepository() {
        if (dynamoDbRepo != null && dynamoDbRepo.isAvailable()) {
            return dynamoDbRepo;
        }
        return inMemoryRepo;
    }

    public String getStorageMode() {
        if (dynamoDbRepo != null && dynamoDbRepo.isAvailable()) {
            return "AWS DynamoDB";
        }
        return "Local Dev Mode";
    }

    public List<TodoItem> getAllTodos() {
        List<TodoItem> items = getActiveRepository().findAll();
        return sortTodos(items);
    }

    public Optional<TodoItem> getTodoById(String id) {
        return getActiveRepository().findById(id);
    }

    public TodoItem createTodo(CreateTodoRequest request) {
        long now = System.currentTimeMillis();
        String priority = (request.priority() != null && !request.priority().isBlank())
                ? request.priority().toUpperCase()
                : "MEDIUM";

        TodoItem item = new TodoItem(
                UUID.randomUUID().toString(),
                request.title().trim(),
                false,
                priority,
                request.dueDate(),
                request.category(),
                now,
                now,
                null
        );

        return getActiveRepository().save(item);
    }

    public Optional<TodoItem> updateTodo(String id, UpdateTodoRequest request) {
        Optional<TodoItem> existingOpt = getActiveRepository().findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        TodoItem item = existingOpt.get();
        item.setTitle(request.title().trim());
        if (request.priority() != null && !request.priority().isBlank()) {
            item.setPriority(request.priority().toUpperCase());
        }
        item.setDueDate(request.dueDate());
        item.setCategory(request.category());
        item.setUpdatedAt(System.currentTimeMillis());

        return Optional.of(getActiveRepository().save(item));
    }

    public Optional<TodoItem> toggleTodo(String id) {
        Optional<TodoItem> existingOpt = getActiveRepository().findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        TodoItem item = existingOpt.get();
        boolean newCompleted = !Boolean.TRUE.equals(item.getCompleted());
        item.setCompleted(newCompleted);
        item.setUpdatedAt(System.currentTimeMillis());

        if (newCompleted) {
            // Set 30 days TTL (in epoch seconds)
            item.setTtl((System.currentTimeMillis() / 1000) + (30L * 24 * 60 * 60));
        } else {
            item.setTtl(null);
        }

        return Optional.of(getActiveRepository().save(item));
    }

    public boolean deleteTodo(String id) {
        Optional<TodoItem> existing = getActiveRepository().findById(id);
        if (existing.isPresent()) {
            getActiveRepository().deleteById(id);
            return true;
        }
        return false;
    }

    public void clearCompleted() {
        getActiveRepository().deleteAllCompleted();
    }

    /**
     * Smart sorting:
     * 1. Active items first, completed items last (pinned to bottom).
     * 2. Active items sorted by Priority (HIGH > MEDIUM > LOW), then earliest Due Date, then newest Created.
     * 3. Completed items sorted by recently updated/completed.
     */
    private List<TodoItem> sortTodos(List<TodoItem> items) {
        List<TodoItem> active = new ArrayList<>();
        List<TodoItem> completed = new ArrayList<>();

        for (TodoItem item : items) {
            if (Boolean.TRUE.equals(item.getCompleted())) {
                completed.add(item);
            } else {
                active.add(item);
            }
        }

        active.sort((a, b) -> {
            // 1. Priority
            int pA = priorityWeight(a.getPriority());
            int pB = priorityWeight(b.getPriority());
            if (pA != pB) return Integer.compare(pA, pB);

            // 2. Due Date (earliest first, nulls last)
            if (a.getDueDate() != null && b.getDueDate() != null) {
                int c = a.getDueDate().compareTo(b.getDueDate());
                if (c != 0) return c;
            } else if (a.getDueDate() != null) {
                return -1;
            } else if (b.getDueDate() != null) {
                return 1;
            }

            // 3. Created At (newest first)
            long tA = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
            long tB = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
            return Long.compare(tB, tA);
        });

        completed.sort((a, b) -> {
            long uA = a.getUpdatedAt() != null ? a.getUpdatedAt() : 0L;
            long uB = b.getUpdatedAt() != null ? b.getUpdatedAt() : 0L;
            return Long.compare(uB, uA);
        });

        List<TodoItem> result = new ArrayList<>(active.size() + completed.size());
        result.addAll(active);
        result.addAll(completed);
        return result;
    }

    private int priorityWeight(String priority) {
        if (priority == null) return 2;
        return switch (priority.toUpperCase()) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 2;
        };
    }
}
