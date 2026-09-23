package com.taskflow.controller;

import com.taskflow.dto.RegisterForm;
import com.taskflow.exception.DuplicateUserException;
import com.taskflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String root(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? "redirect:/tasks"
                : "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                            BindingResult bindingResult,
                            Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form);
        } catch (DuplicateUserException ex) {
            model.addAttribute("serverError", ex.getMessage());
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
