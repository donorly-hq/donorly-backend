package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.DonorNoteRequest;
import org.donorly.backend.dto.DonorNoteResponse;
import org.donorly.backend.model.DonorNote;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.DonorNoteRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonorNoteService {

    private final DonorNoteRepository noteRepository;
    private final DonorService donorService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public List<DonorNoteResponse> listNotes(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);
        return noteRepository.findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(orgId, donorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DonorNoteResponse addNote(UUID donorId, DonorNoteRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);

        DonorNote note = new DonorNote();
        note.setOrganizationId(orgId);
        note.setDonorId(donorId);
        note.setNoteText(request.noteText());
        note.setNoteType(request.noteType());
        if (request.visibility() != null) {
            note.setVisibility(request.visibility());
        }
        note.setCreatedBy(TenantContext.getUserId());

        DonorNote saved = noteRepository.save(note);
        auditService.record("donor_note.create", "donor_note", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteNote(UUID donorId, UUID noteId) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);
        DonorNote note = noteRepository.findById(noteId)
                .filter(n -> n.getOrganizationId().equals(orgId) && n.getDonorId().equals(donorId))
                .orElseThrow(() -> new NotFoundException("Note not found"));
        noteRepository.delete(note);
        auditService.record("donor_note.delete", "donor_note", noteId);
    }

    private DonorNoteResponse toResponse(DonorNote note) {
        String createdByName = null;
        if (note.getCreatedBy() != null) {
            createdByName = userRepository.findById(note.getCreatedBy())
                    .map(User::getFullName)
                    .orElse(null);
        }
        return new DonorNoteResponse(
                note.getId(),
                note.getDonorId(),
                note.getNoteText(),
                note.getNoteType(),
                note.getVisibility(),
                note.getCreatedBy(),
                createdByName,
                note.getCreatedAt()
        );
    }
}
