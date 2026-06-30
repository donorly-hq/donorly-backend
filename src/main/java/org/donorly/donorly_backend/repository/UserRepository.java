package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRole(String role);
    Optional<User> findByEmailAndRole(String email, String role);
    Optional<User> findByAmbassadorId(String ambassadorId);
}
