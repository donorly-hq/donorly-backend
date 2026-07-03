package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CallLogRepository extends JpaRepository<CallLog, String> {
    List<CallLog> findByDonorId(String donorId);
    List<CallLog> findByAmbassadorId(String ambassadorId);
}
