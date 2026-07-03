package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Ambassador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AmbassadorRepository extends JpaRepository<Ambassador, String> {
    Optional<Ambassador> findByEmail(String email);
    List<Ambassador> findByParentAmbassadorId(String parentAmbassadorId);

    @Query("SELECT a FROM Ambassador a JOIN a.ancestorPath ap WHERE ap = :ancestorId")
    List<Ambassador> findByAncestorPathContains(@Param("ancestorId") String ancestorId);
}
