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
    @PostMapping("/sameer")
    public String createSameer() {
        userRepository.findByEmail("sameer-amb@donorly.org").ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("sameer123"));
            u.setRole("AMBASSADOR");
            u.setActive(true);
            u.setAmbassadorId("1d6aa07c-9c32-47fb-9cae-93274e71f62c");
            userRepository.save(u);
        });
        if (userRepository.findByEmail("sameer-amb@donorly.org").isEmpty()) {
            User u = new User();
            u.setEmail("sameer-amb@donorly.org");
            u.setPassword(passwordEncoder.encode("sameer123"));
            u.setRole("AMBASSADOR");
            u.setActive(true);
            u.setAmbassadorId("1d6aa07c-9c32-47fb-9cae-93274e71f62c");
            userRepository.save(u);
        }
        return "Sameer created!";
    }
}
