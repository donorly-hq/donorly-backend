package org.donorly.donorly_backend.service;
import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public List<User> getAll() { return userRepository.findAll(); }
    public Optional<User> getById(String id) { return userRepository.findById(id); }
    public Optional<User> getByEmail(String email) { return userRepository.findByEmail(email); }
    public User save(User user) { return userRepository.save(user); }
    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    public User update(String id, User updated) {
        updated.setId(id);
        return userRepository.save(updated);
    }
    public void delete(String id) { userRepository.deleteById(id); }
}
