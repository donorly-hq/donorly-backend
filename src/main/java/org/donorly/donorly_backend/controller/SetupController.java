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
    @PostMapping("/zaid")
    public String createZaid() {
        userRepository.findByEmail("zaid@donorly.org").ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("zaid123"));
            u.setRole("AMBASSADOR");
            u.setActive(true);
            u.setAmbassadorId("62880574-52d7-4e3c-94f1-b072af799973");
            userRepository.save(u);
        });
        return "Zaid password reset!";
    }
}
