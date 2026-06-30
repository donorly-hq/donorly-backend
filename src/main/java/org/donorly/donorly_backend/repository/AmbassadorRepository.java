package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Ambassador;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AmbassadorRepository extends MongoRepository<Ambassador, String> {
    Optional<Ambassador> findByCode(String code);
    List<Ambassador> findByStatus(String status);
    List<Ambassador> findByParentAmbassadorId(String parentAmbassadorId);
    List<Ambassador> findByAncestorPathContains(String ambassadorId);
}
