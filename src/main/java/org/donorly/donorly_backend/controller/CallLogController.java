package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.CallLog;
import org.donorly.donorly_backend.service.CallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/call-logs")
@CrossOrigin(origins = "*")
public class CallLogController {

    @Autowired
    private CallLogService callLogService;

    @GetMapping
    public List<CallLog> getAll() {
        return callLogService.getAllCallLogs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallLog> getById(@PathVariable String id) {
        return callLogService.getCallLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/donor/{donorId}")
    public List<CallLog> getByDonor(@PathVariable String donorId) {
        return callLogService.getCallLogsByDonor(donorId);
    }

    @PostMapping
    public CallLog create(@RequestBody CallLog callLog) {
        return callLogService.createCallLog(callLog);
    }

    @PutMapping("/{id}")
    public CallLog update(@PathVariable String id, @RequestBody CallLog callLog) {
        return callLogService.updateCallLog(id, callLog);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        callLogService.deleteCallLog(id);
        return ResponseEntity.ok().build();
    }
}
