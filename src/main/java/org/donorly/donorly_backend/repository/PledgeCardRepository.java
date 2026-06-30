package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.PledgeCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PledgeCardRepository extends MongoRepository<PledgeCard, String> {
    List<PledgeCard> findByStatus(String status);
    List<PledgeCard> findByDonorId(String donorId);
    List<PledgeCard> findByAmbassadorId(String ambassadorId);
    List<PledgeCard> findByVolunteerId(String volunteerId);
}
