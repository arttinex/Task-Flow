package com.taskflow.service;

import com.taskflow.dto.RegisterForm;
import com.taskflow.exception.DuplicateUserException;
import com.taskflow.model.User;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterForm form) {
        // Checked here for a friendly form error; the unique index on
        // User.username/email is still the last line of defense against a
        // race between two concurrent registrations for the same name.
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new DuplicateUserException("That username is already taken.");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new DuplicateUserException("An account with that email already exists.");
        }

        User user = User.builder()
                .username(form.getUsername().trim())
                .email(form.getEmail().trim().toLowerCase())
                .fullName(form.getFullName().trim())
                .password(passwordEncoder.encode(form.getPassword()))
                .createdAt(Instant.now())
                .build();

        return userRepository.save(user);
    }
}
