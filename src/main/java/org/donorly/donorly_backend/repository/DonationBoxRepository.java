package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.DonationBox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationBoxRepository extends JpaRepository<DonationBox, String> {
    List<DonationBox> findByAssignedAmbassadorId(String ambassadorId);
}
