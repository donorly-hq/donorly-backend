package org.donorly.donorly_backend.controller;
import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping public List<User> getAll() { return userService.getAll(); }
    @GetMapping("/{id}") public ResponseEntity<User> getById(@PathVariable String id) { return userService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @GetMapping("/email/{email}") public ResponseEntity<User> getByEmail(@PathVariable String email) { return userService.getByEmail(email).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping public User create(@RequestBody User user) { return userService.create(user); }
    @PutMapping("/{id}") public User update(@PathVariable String id, @RequestBody User user) { return userService.update(id, user); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id) { userService.delete(id); return ResponseEntity.noContent().build(); }
}
