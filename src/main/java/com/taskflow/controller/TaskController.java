package com.taskflow.controller;

import com.taskflow.dto.TaskForm;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.CustomUserDetails;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public String list(@RequestParam(required = false) TaskStatus status,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        Model model) {
        model.addAttribute("tasks", taskService.getTasksForUser(principal.getId(), status));
        model.addAttribute("counts", taskService.getStatusCounts(principal.getId()));
        model.addAttribute("activeFilter", status);
        model.addAttribute("fullName", principal.getFullName());
        return "tasks/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("isEdit", false);
        return "tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("taskForm") TaskForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "tasks/form";
        }
        taskService.createTask(form, principal.getId());
        return "redirect:/tasks";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            Model model) {
        Task task = taskService.getOwnedTaskOrThrow(id, principal.getId());
        TaskForm form = new TaskForm();
        form.setId(task.getId());
        form.setTitle(task.getTitle());
        form.setDescription(task.getDescription());
        form.setDueDate(task.getDueDate());
        form.setPriority(task.getPriority());
        form.setStatus(task.getStatus());
        model.addAttribute("taskForm", form);
        model.addAttribute("isEdit", true);
        return "tasks/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id,
                          @Valid @ModelAttribute("taskForm") TaskForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "tasks/form";
        }
        taskService.updateTask(id, form, principal.getId());
        return "redirect:/tasks";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id,
                          @AuthenticationPrincipal CustomUserDetails principal) {
        taskService.deleteTask(id, principal.getId());
        return "redirect:/tasks";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable String id,
                          @AuthenticationPrincipal CustomUserDetails principal) {
        taskService.toggleStatus(id, principal.getId());
        return "redirect:/tasks";
    }
}
