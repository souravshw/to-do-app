package com.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoRequest(
    @NotBlank(message = "Task description cannot be blank")
    @Size(max = 250, message = "Task description must be at most 250 characters")
    String title,
    String priority,
    String dueDate,
    String category
) {}
