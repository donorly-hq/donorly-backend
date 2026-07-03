package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonorRepository extends JpaRepository<Donor, String> {
    List<Donor> findByAmbassadorId(String ambassadorId);
}
