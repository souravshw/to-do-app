package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.UpdateTodoRequest;
import com.example.todo.model.TodoItem;
import com.example.todo.repository.DynamoDbTodoRepository;
import com.example.todo.repository.InMemoryTodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TodoServiceTest {

    private InMemoryTodoRepository inMemoryRepo;
    private DynamoDbTodoRepository dynamoDbRepo;
    private TodoService todoService;

    @BeforeEach
    void setUp() {
        inMemoryRepo = new InMemoryTodoRepository();
        dynamoDbRepo = Mockito.mock(DynamoDbTodoRepository.class);
        when(dynamoDbRepo.isAvailable()).thenReturn(false); // test with fallback repo
        todoService = new TodoService(dynamoDbRepo, inMemoryRepo);
    }

    @Test
    void testCreateTodo() {
        CreateTodoRequest request = new CreateTodoRequest("Buy milk", "HIGH", "2026-10-15", "Errands");
        TodoItem created = todoService.createTodo(request);

        assertNotNull(created.getId());
        assertEquals("Buy milk", created.getTitle());
        assertEquals("HIGH", created.getPriority());
        assertEquals("2026-10-15", created.getDueDate());
        assertEquals("Errands", created.getCategory());
        assertFalse(created.getCompleted());
        assertNull(created.getTtl());
    }

    @Test
    void testToggleTodoSetsAndClearsTtl() {
        CreateTodoRequest request = new CreateTodoRequest("Fix bug", "MEDIUM", null, "Work");
        TodoItem created = todoService.createTodo(request);

        // Toggle to completed
        Optional<TodoItem> toggled = todoService.toggleTodo(created.getId());
        assertTrue(toggled.isPresent());
        assertTrue(toggled.get().getCompleted());
        assertNotNull(toggled.get().getTtl(), "TTL must be set upon completion for DynamoDB retention");

        // Toggle back to active
        Optional<TodoItem> toggledBack = todoService.toggleTodo(created.getId());
        assertTrue(toggledBack.isPresent());
        assertFalse(toggledBack.get().getCompleted());
        assertNull(toggledBack.get().getTtl(), "TTL must be cleared when marked active");
    }

    @Test
    void testUpdateTodoTitleAndFields() {
        CreateTodoRequest request = new CreateTodoRequest("Initial title", "LOW", null, null);
        TodoItem created = todoService.createTodo(request);

        UpdateTodoRequest update = new UpdateTodoRequest("Updated title", "HIGH", "2026-11-01", "UpdatedCat");
        Optional<TodoItem> updated = todoService.updateTodo(created.getId(), update);

        assertTrue(updated.isPresent());
        assertEquals("Updated title", updated.get().getTitle());
        assertEquals("HIGH", updated.get().getPriority());
        assertEquals("2026-11-01", updated.get().getDueDate());
        assertEquals("UpdatedCat", updated.get().getCategory());
    }

    @Test
    void testSmartSortingActiveFirstAndCompletedPinnedToBottom() {
        // Clear seeded items for clean sort test
        inMemoryRepo.deleteAllCompleted();
        for (TodoItem item : inMemoryRepo.findAll()) {
            inMemoryRepo.deleteById(item.getId());
        }

        TodoItem lowActive = todoService.createTodo(new CreateTodoRequest("Low active", "LOW", "2026-10-30", null));
        TodoItem highActive = todoService.createTodo(new CreateTodoRequest("High active", "HIGH", "2026-10-20", null));
        TodoItem medActive = todoService.createTodo(new CreateTodoRequest("Med active", "MEDIUM", "2026-10-25", null));
        TodoItem completedTask = todoService.createTodo(new CreateTodoRequest("Completed task", "HIGH", "2026-10-10", null));
        todoService.toggleTodo(completedTask.getId());

        List<TodoItem> sorted = todoService.getAllTodos();

        assertEquals(4, sorted.size());
        // High active first
        assertEquals("High active", sorted.get(0).getTitle());
        // Med active second
        assertEquals("Med active", sorted.get(1).getTitle());
        // Low active third
        assertEquals("Low active", sorted.get(2).getTitle());
        // Completed task pinned to bottom
        assertEquals("Completed task", sorted.get(3).getTitle());
        assertTrue(sorted.get(3).getCompleted());
    }

    @Test
    void testDeleteTodo() {
        CreateTodoRequest request = new CreateTodoRequest("Temporary item", "LOW", null, null);
        TodoItem created = todoService.createTodo(request);

        boolean deleted = todoService.deleteTodo(created.getId());
        assertTrue(deleted);
        assertTrue(todoService.getTodoById(created.getId()).isEmpty());
    }
}
