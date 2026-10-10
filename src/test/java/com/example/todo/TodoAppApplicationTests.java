package com.example.todo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "aws.dynamodb.enabled=false"
})
class TodoAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
