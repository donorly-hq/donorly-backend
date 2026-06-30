package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.CallLog;
import org.donorly.donorly_backend.repository.CallLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CallLogService {

    @Autowired
    private CallLogRepository callLogRepository;

    public List<CallLog> getAllCallLogs() {
        return callLogRepository.findAll();
    }

    public Optional<CallLog> getCallLogById(String id) {
        return callLogRepository.findById(id);
    }

    public List<CallLog> getCallLogsByDonor(String donorId) {
        return callLogRepository.findByDonorId(donorId);
    }

    public CallLog createCallLog(CallLog callLog) {
        callLog.setCalledAt(LocalDateTime.now());
        return callLogRepository.save(callLog);
    }

    public CallLog updateCallLog(String id, CallLog updated) {
        updated.setId(id);
        return callLogRepository.save(updated);
    }

    public void deleteCallLog(String id) {
        callLogRepository.deleteById(id);
    }
}
