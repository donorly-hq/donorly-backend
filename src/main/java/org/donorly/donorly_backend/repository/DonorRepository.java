package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Donor;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DonorRepository extends MongoRepository<Donor, String> {
    List<Donor> findByAmbassadorId(String ambassadorId);
    List<Donor> findByStatus(String status);
    List<Donor> findByDonationType(String donationType);
}
