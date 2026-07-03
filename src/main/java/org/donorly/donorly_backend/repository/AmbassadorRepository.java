package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Ambassador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbassadorRepository extends JpaRepository<Ambassador, UUID> {

    Optional<Ambassador> findByEmailAddress(String emailAddress);

    List<Ambassador> findByParentAmbassadorId(UUID parentAmbassadorId);

    @Query("SELECT a FROM Ambassador a JOIN a.ancestorPath ap WHERE ap = :ancestorId")
    List<Ambassador> findByAncestorPathContains(@Param("ancestorId") UUID ancestorId);
}
