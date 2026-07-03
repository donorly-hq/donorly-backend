package org.donorly.donorly_backend.controller;
import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @PostMapping("/admin")
    public String createAdmin() {
        if (userRepository.findByEmail("admin@donorly.org").isPresent()) {
            User u = userRepository.findByEmail("admin@donorly.org").get();
            u.setPassword(passwordEncoder.encode("admin123"));
            u.setRole("ADMIN");
            u.setActive(true);
            userRepository.save(u);
            return "Admin password reset!";
        }
        User u = new User();
        u.setEmail("admin@donorly.org");
        u.setPassword(passwordEncoder.encode("admin123"));
        u.setRole("ADMIN");
        u.setActive(true);
        userRepository.save(u);
        return "Admin created!";
    }
}
