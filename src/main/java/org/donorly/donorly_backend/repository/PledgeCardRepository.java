package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.PledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PledgeCardRepository extends JpaRepository<PledgeCard, String> {
    List<PledgeCard> findByDonorId(String donorId);
    List<PledgeCard> findByAmbassadorId(String ambassadorId);
}
