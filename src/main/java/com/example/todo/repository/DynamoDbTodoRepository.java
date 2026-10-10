package com.example.todo.repository;

import com.example.todo.config.DynamoDbProperties;
import com.example.todo.model.TodoItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository("dynamoDbTodoRepository")
public class DynamoDbTodoRepository implements TodoRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTodoRepository.class);

    private final DynamoDbProperties properties;
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<TodoItem> table;
    private volatile boolean available = false;

    @Autowired
    public DynamoDbTodoRepository(DynamoDbProperties properties,
                                  @Autowired(required = false) DynamoDbClient dynamoDbClient,
                                  @Autowired(required = false) DynamoDbEnhancedClient enhancedClient) {
        this.properties = properties;
        this.dynamoDbClient = dynamoDbClient;
        this.enhancedClient = enhancedClient;
    }

    @PostConstruct
    public void init() {
        if (dynamoDbClient == null || enhancedClient == null || !properties.isEnabled()) {
            log.info("AWS DynamoDB is not enabled or clients not configured; using offline mode.");
            available = false;
            return;
        }

        try {
            this.table = enhancedClient.table(properties.getTableName(), TableSchema.fromBean(TodoItem.class));
            ensureTableExists();
            available = true;
            log.info("Successfully connected to DynamoDB table '{}'", properties.getTableName());
        } catch (Exception e) {
            log.warn("Could not connect to DynamoDB ({}). Falling back to in-memory store for local execution.", e.getMessage());
            available = false;
        }
    }

    private void ensureTableExists() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(properties.getTableName())
                    .build());
            log.info("DynamoDB table '{}' already exists.", properties.getTableName());
        } catch (ResourceNotFoundException e) {
            if (properties.isCreateTableIfNotExists()) {
                log.info("DynamoDB table '{}' not found. Creating table with on-demand capacity...", properties.getTableName());
                table.createTable();
                log.info("Table '{}' creation requested.", properties.getTableName());
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<TodoItem> findAll() {
        if (!available) return List.of();
        try {
            List<TodoItem> items = new ArrayList<>();
            table.scan().items().forEach(items::add);
            return items;
        } catch (Exception e) {
            log.error("DynamoDB scan error: {}", e.getMessage());
            throw new RuntimeException("Failed to scan DynamoDB items", e);
        }
    }

    @Override
    public Optional<TodoItem> findById(String id) {
        if (!available) return Optional.empty();
        try {
            TodoItem item = table.getItem(Key.builder().partitionValue(id).build());
            return Optional.ofNullable(item);
        } catch (Exception e) {
            log.error("DynamoDB getItem error for id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public TodoItem save(TodoItem item) {
        if (!available) {
            throw new IllegalStateException("DynamoDB repository is not available");
        }
        try {
            table.putItem(item);
            return item;
        } catch (Exception e) {
            log.error("DynamoDB putItem error: {}", e.getMessage());
            throw new RuntimeException("Failed to persist item to DynamoDB", e);
        }
    }

    @Override
    public void deleteById(String id) {
        if (!available) return;
        try {
            table.deleteItem(Key.builder().partitionValue(id).build());
        } catch (Exception e) {
            log.error("DynamoDB deleteItem error for id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete item from DynamoDB", e);
        }
    }

    @Override
    public void deleteAllCompleted() {
        if (!available) return;
        try {
            for (TodoItem item : findAll()) {
                if (Boolean.TRUE.equals(item.getCompleted())) {
                    deleteById(item.getId());
                }
            }
        } catch (Exception e) {
            log.error("DynamoDB deleteAllCompleted error: {}", e.getMessage());
            throw new RuntimeException("Failed to delete completed items from DynamoDB", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
