package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailAddress(String emailAddress);
}
