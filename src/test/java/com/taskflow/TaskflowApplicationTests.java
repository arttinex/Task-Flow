package com.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the full context against an embedded/in-memory-style config so CI
 * catches wiring mistakes (missing beans, bad property keys) without
 * needing a real MongoDB instance for this smoke test.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/taskflow-test"
})
class TaskflowApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a failed context load fails this test.
    }
}
