package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.AppUser;
import org.donorly.donorly_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<AppUser> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getById(@PathVariable UUID id) {
        return userService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<AppUser> getByEmail(@PathVariable String email) {
        return userService.getByEmail(email).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Creates the account AND sends the welcome email with credentials.
    @PostMapping
    public AppUser create(@RequestBody AppUser user) {
        return userService.createWithWelcomeEmail(user);
    }

    @PutMapping("/{id}")
    public AppUser update(@PathVariable UUID id, @RequestBody AppUser user) {
        return userService.update(id, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
