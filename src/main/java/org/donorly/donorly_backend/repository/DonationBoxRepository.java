package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.DonationBox;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DonationBoxRepository extends MongoRepository<DonationBox, String> {
    List<DonationBox> findByStatus(String status);
    List<DonationBox> findByVolunteerId(String volunteerId);
    List<DonationBox> findByArea(String area);
}
