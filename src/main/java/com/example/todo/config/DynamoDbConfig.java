package com.example.todo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    private final DynamoDbProperties properties;

    public DynamoDbConfig(DynamoDbProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true", matchIfMissing = true)
    public DynamoDbClient dynamoDbClient() {
        log.info("Initializing AWS DynamoDB Client for region: {}, table: {}", 
                properties.getRegion(), properties.getTableName());
        
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(properties.getRegion()));

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            log.info("Using custom DynamoDB endpoint: {}", properties.getEndpoint());
            builder.endpointOverride(URI.create(properties.getEndpoint()));
            // If targeting a local endpoint like localhost:8000, supply dummy credentials
            if (properties.getEndpoint().contains("localhost") || properties.getEndpoint().contains("127.0.0.1")) {
                builder.credentialsProvider(AnonymousCredentialsProvider.create());
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true", matchIfMissing = true)
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
