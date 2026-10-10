package com.example.todo.repository;

import com.example.todo.model.TodoItem;

import java.util.List;
import java.util.Optional;

public interface TodoRepository {
    List<TodoItem> findAll();
    Optional<TodoItem> findById(String id);
    TodoItem save(TodoItem item);
    void deleteById(String id);
    void deleteAllCompleted();
    boolean isAvailable();
}
