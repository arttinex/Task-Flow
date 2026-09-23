package com.taskflow.service;

import com.taskflow.config.CacheConfig;
import com.taskflow.dto.TaskForm;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * Every task list view (and every filtered view) reads from here first.
     * Cached per-user so re-opening the board doesn't hit MongoDB again
     * until something actually changes for that user.
     */
    @Cacheable(value = CacheConfig.TASKS_CACHE, key = "#userId")
    public List<Task> getTasksForUser(String userId) {
        return taskRepository.findByUserIdOrderByDueDateAscCreatedAtDesc(userId);
    }

    public List<Task> getTasksForUser(String userId, TaskStatus statusFilter) {
        List<Task> all = getTasksForUser(userId);
        if (statusFilter == null) {
            return all;
        }
        return all.stream().filter(t -> t.getStatus() == statusFilter).toList();
    }

    /** Keyed by enum name (String) rather than the enum itself, so views can
     * look counts up with a plain string key ({@code counts.get('PENDING')}). */
    public Map<String, Long> getStatusCounts(String userId) {
        List<Task> all = getTasksForUser(userId);
        return Map.of(
                TaskStatus.PENDING.name(), all.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count(),
                TaskStatus.IN_PROGRESS.name(), all.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count(),
                TaskStatus.COMPLETED.name(), all.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count()
        );
    }

    public Task getOwnedTaskOrThrow(String taskId, String userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
    }

    @CacheEvict(value = CacheConfig.TASKS_CACHE, key = "#userId")
    public Task createTask(TaskForm form, String userId) {
        Task task = Task.builder()
                .userId(userId)
                .title(form.getTitle().trim())
                .description(form.getDescription() == null ? null : form.getDescription().trim())
                .dueDate(form.getDueDate())
                .priority(form.getPriority())
                .status(form.getStatus() == null ? TaskStatus.PENDING : form.getStatus())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return taskRepository.save(task);
    }

    @CacheEvict(value = CacheConfig.TASKS_CACHE, key = "#userId")
    public Task updateTask(String taskId, TaskForm form, String userId) {
        Task existing = getOwnedTaskOrThrow(taskId, userId);
        existing.setTitle(form.getTitle().trim());
        existing.setDescription(form.getDescription() == null ? null : form.getDescription().trim());
        existing.setDueDate(form.getDueDate());
        existing.setPriority(form.getPriority());
        existing.setStatus(form.getStatus());
        existing.setUpdatedAt(Instant.now());
        return taskRepository.save(existing);
    }

    @CacheEvict(value = CacheConfig.TASKS_CACHE, key = "#userId")
    public void deleteTask(String taskId, String userId) {
        // Guarantees the delete itself is scoped to the owner — even if a
        // caller somehow bypassed the earlier ownership check.
        taskRepository.deleteByIdAndUserId(taskId, userId);
    }

    @CacheEvict(value = CacheConfig.TASKS_CACHE, key = "#userId")
    public Task toggleStatus(String taskId, String userId) {
        Task task = getOwnedTaskOrThrow(taskId, userId);
        TaskStatus next = switch (task.getStatus()) {
            case PENDING -> TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> TaskStatus.COMPLETED;
            case COMPLETED -> TaskStatus.PENDING;
        };
        task.setStatus(next);
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }
}
