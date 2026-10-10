package com.example.todo.repository;

import com.example.todo.model.TodoItem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository("inMemoryTodoRepository")
public class InMemoryTodoRepository implements TodoRepository {

    private final Map<String, TodoItem> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void seedInitialData() {
        long now = System.currentTimeMillis();
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        TodoItem t1 = new TodoItem(
                UUID.randomUUID().toString(),
                "Design CloudFormation template for DynamoDB & IAM",
                false,
                "HIGH",
                today,
                "DevOps",
                now - 3600000,
                now - 3600000,
                null
        );

        TodoItem t2 = new TodoItem(
                UUID.randomUUID().toString(),
                "Review DynamoDB TTL retention & partition keys",
                false,
                "MEDIUM",
                tomorrow,
                "Backend",
                now - 7200000,
                now - 7200000,
                null
        );

        TodoItem t3 = new TodoItem(
                UUID.randomUUID().toString(),
                "Implement optimistic UI updates in Vanilla JavaScript",
                false,
                "HIGH",
                today,
                "Frontend",
                now - 10800000,
                now - 10800000,
                null
        );

        TodoItem t4 = new TodoItem(
                UUID.randomUUID().toString(),
                "Setup Spring Boot 3 & Maven wrapper project structure",
                true,
                "LOW",
                today,
                "Setup",
                now - 14400000,
                now - 1800000,
                (now / 1000) + (30L * 24 * 60 * 60)
        );

        store.put(t1.getId(), t1);
        store.put(t2.getId(), t2);
        store.put(t3.getId(), t3);
        store.put(t4.getId(), t4);
    }

    @Override
    public List<TodoItem> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<TodoItem> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public TodoItem save(TodoItem item) {
        store.put(item.getId(), item);
        return item;
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public void deleteAllCompleted() {
        store.entrySet().removeIf(entry -> Boolean.TRUE.equals(entry.getValue().getCompleted()));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
