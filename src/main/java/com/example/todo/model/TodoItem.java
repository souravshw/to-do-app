package com.example.todo.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Objects;

@DynamoDbBean
public class TodoItem {
    private String id;
    private String title;
    private Boolean completed;
    private String priority; // HIGH, MEDIUM, LOW
    private String dueDate;  // YYYY-MM-DD
    private String category; // e.g. Work, Personal, Shopping
    private Long createdAt;  // epoch milliseconds
    private Long updatedAt;  // epoch milliseconds
    private Long ttl;        // epoch seconds for DynamoDB TTL

    public TodoItem() {
    }

    public TodoItem(String id, String title, Boolean completed, String priority, String dueDate, String category, Long createdAt, Long updatedAt, Long ttl) {
        this.id = id;
        this.title = title;
        this.completed = completed != null ? completed : false;
        this.priority = (priority != null && !priority.isBlank()) ? priority : "MEDIUM";
        this.dueDate = dueDate;
        this.category = category;
        this.createdAt = createdAt != null ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt != null ? updatedAt : System.currentTimeMillis();
        this.ttl = ttl;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TodoItem todoItem = (TodoItem) o;
        return Objects.equals(id, todoItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
