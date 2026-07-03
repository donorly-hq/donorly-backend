package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.CallLog;
import org.donorly.donorly_backend.repository.CallLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallLogService {

    private final CallLogRepository callLogRepository;

    public List<CallLog> getAll() {
        return callLogRepository.findAll();
    }

    public Optional<CallLog> getById(String id) {
        return callLogRepository.findById(id);
    }

    public List<CallLog> getByDonorId(String donorId) {
        return callLogRepository.findByDonorId(donorId);
    }

    public List<CallLog> getByAmbassadorId(String ambassadorId) {
        return callLogRepository.findByAmbassadorId(ambassadorId);
    }

    public CallLog save(CallLog callLog) {
        return callLogRepository.save(callLog);
    }

    public void delete(String id) {
        callLogRepository.deleteById(id);
    }
}
