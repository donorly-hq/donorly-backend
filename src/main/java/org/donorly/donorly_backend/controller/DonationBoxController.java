package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.CallLog;
import org.donorly.donorly_backend.service.CallLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/call-logs")
@RequiredArgsConstructor
public class CallLogController {

    private final CallLogService callLogService;

    @GetMapping
    public List<CallLog> getAll() {
        return callLogService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallLog> getById(@PathVariable String id) {
        return callLogService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/donor/{donorId}")
    public List<CallLog> getByDonor(@PathVariable String donorId) {
        return callLogService.getByDonorId(donorId);
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<CallLog> getByAmbassador(@PathVariable String ambassadorId) {
        return callLogService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public CallLog create(@RequestBody CallLog callLog) {
        return callLogService.save(callLog);
    }

    @PutMapping("/{id}")
    public CallLog update(@PathVariable String id, @RequestBody CallLog callLog) {
        callLog.setId(id);
        return callLogService.save(callLog);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        callLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
