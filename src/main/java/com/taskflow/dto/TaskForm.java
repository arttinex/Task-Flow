package com.taskflow.dto;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TaskForm {

    private String id;

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be under 120 characters")
    private String title;

    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    private Priority priority = Priority.MEDIUM;

    private TaskStatus status = TaskStatus.PENDING;
}
