package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.UpdateTodoRequest;
import com.example.todo.model.TodoItem;
import com.example.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TodoApiController {

    private final TodoService todoService;

    public TodoApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/todos")
    public ResponseEntity<List<TodoItem>> getAllTodos() {
        return ResponseEntity.ok(todoService.getAllTodos());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        List<TodoItem> all = todoService.getAllTodos();
        long completed = all.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        long active = all.size() - completed;

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "storageMode", todoService.getStorageMode(),
                "totalCount", all.size(),
                "activeCount", active,
                "completedCount", completed
        ));
    }

    @PostMapping("/todos")
    public ResponseEntity<TodoItem> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        TodoItem created = todoService.createTodo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/todos/{id}")
    public ResponseEntity<TodoItem> updateTodo(@PathVariable String id,
                                               @Valid @RequestBody UpdateTodoRequest request) {
        return todoService.updateTodo(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/todos/{id}/toggle")
    public ResponseEntity<TodoItem> toggleTodo(@PathVariable String id) {
        return todoService.toggleTodo(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/todos/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable String id) {
        boolean deleted = todoService.deleteTodo(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/todos/completed")
    public ResponseEntity<Void> clearCompleted() {
        todoService.clearCompleted();
        return ResponseEntity.noContent().build();
    }
}
