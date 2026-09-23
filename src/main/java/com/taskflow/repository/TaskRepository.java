package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends MongoRepository<Task, String> {

    // Backed by the index on userId — never scans the full collection.
    List<Task> findByUserIdOrderByDueDateAscCreatedAtDesc(String userId);

    List<Task> findByUserIdAndStatusOrderByDueDateAscCreatedAtDesc(String userId, TaskStatus status);

    // Ownership check baked into the query itself, so a user can never load,
    // edit or delete another user's task by guessing an id.
    Optional<Task> findByIdAndUserId(String id, String userId);

    long countByUserIdAndStatus(String userId, TaskStatus status);

    long countByUserId(String userId);

    void deleteByIdAndUserId(String id, String userId);
}
