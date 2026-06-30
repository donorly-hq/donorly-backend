package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.CallLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CallLogRepository extends MongoRepository<CallLog, String> {
    List<CallLog> findByDonorId(String donorId);
    List<CallLog> findByPledgeCardId(String pledgeCardId);
    List<CallLog> findByCallerId(String callerId);
}
