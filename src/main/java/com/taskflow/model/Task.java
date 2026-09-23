package com.taskflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A single task owned by exactly one user. `userId` is indexed since every
 * read path (list, count, edit, delete) filters by it first — without the
 * index each of those becomes a full collection scan as data grows.
 */
@Document(collection = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    private String description;

    private LocalDate dueDate;

    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
