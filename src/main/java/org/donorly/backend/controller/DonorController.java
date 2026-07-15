package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.AssignmentRequest;
import org.donorly.backend.dto.AssignmentResponse;
import org.donorly.backend.dto.DonorDetailResponse;
import org.donorly.backend.dto.DonorNoteRequest;
import org.donorly.backend.dto.DonorNoteResponse;
import org.donorly.backend.dto.DonorProfileRequest;
import org.donorly.backend.dto.DonorProfileResponse;
import org.donorly.backend.dto.DonorRequest;
import org.donorly.backend.dto.DonorResponse;
import org.donorly.backend.dto.DonorTagResponse;
import org.donorly.backend.service.Donor360Service;
import org.donorly.backend.service.DonorAssignmentService;
import org.donorly.backend.service.DonorNoteService;
import org.donorly.backend.service.DonorProfileService;
import org.donorly.backend.service.DonorService;
import org.donorly.backend.service.DonorTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;
    private final DonorAssignmentService assignmentService;
    private final Donor360Service donor360Service;
    private final DonorProfileService profileService;
    private final DonorNoteService noteService;
    private final DonorTagService tagService;
    private final org.donorly.backend.service.DonorImportService importService;
    private final org.donorly.backend.service.DonorMergeService mergeService;

    /**
     * Without {@code page}, returns the full list (legacy behavior, used by dropdowns).
     * With {@code page}, returns a {@code PageResponse} envelope; {@code q} filters by
     * name/email/phone/city.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('donors.read')")
    public Object list(@RequestParam(required = false) Integer page,
                       @RequestParam(defaultValue = "50") int size,
                       @RequestParam(required = false) String q) {
        if (page == null) {
            return donorService.list().stream().map(DonorResponse::from).toList();
        }
        return donorService.page(page, size, q).map(DonorResponse::from);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('donors.read')")
    public List<DonorResponse> myDonors() {
        return assignmentService.myDonors().stream().map(DonorResponse::from).toList();
    }

    // ---- Import / duplicates / merge -------------------------------------

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('donors.write')")
    public org.donorly.backend.dto.DonorImportResult importDonors(
            @Valid @RequestBody org.donorly.backend.dto.DonorImportRequest request) {
        return importService.importDonors(request);
    }

    @GetMapping("/duplicates")
    @PreAuthorize("hasAuthority('donors.write')")
    public List<org.donorly.backend.dto.DuplicateGroupResponse> duplicates() {
        return mergeService.findDuplicates();
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('donors.delete')")
    public DonorResponse merge(@Valid @RequestBody org.donorly.backend.dto.DonorMergeRequest request) {
        return DonorResponse.from(mergeService.merge(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('donors.read')")
    public DonorResponse get(@PathVariable UUID id) {
        return DonorResponse.from(donorService.get(id));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAuthority('donors.read')")
    public DonorDetailResponse detail(@PathVariable UUID id) {
        return donor360Service.getDetail(id);
    }

    // ---- Profile --------------------------------------------------------

    @GetMapping("/{donorId}/profile")
    @PreAuthorize("hasAuthority('donors.read')")
    public DonorProfileResponse getProfile(@PathVariable UUID donorId) {
        return profileService.getProfile(donorId);
    }

    @PutMapping("/{donorId}/profile")
    @PreAuthorize("hasAuthority('donors.write')")
    public DonorProfileResponse upsertProfile(@PathVariable UUID donorId,
                                              @RequestBody DonorProfileRequest request) {
        return profileService.upsertProfile(donorId, request);
    }

    // ---- Notes ----------------------------------------------------------

    @GetMapping("/{donorId}/notes")
    @PreAuthorize("hasAuthority('donors.read')")
    public List<DonorNoteResponse> listNotes(@PathVariable UUID donorId) {
        return noteService.listNotes(donorId);
    }

    @PostMapping("/{donorId}/notes")
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<DonorNoteResponse> addNote(@PathVariable UUID donorId,
                                                     @Valid @RequestBody DonorNoteRequest request) {
        return ResponseEntity.ok(noteService.addNote(donorId, request));
    }

    @DeleteMapping("/{donorId}/notes/{noteId}")
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<?> deleteNote(@PathVariable UUID donorId, @PathVariable UUID noteId) {
        noteService.deleteNote(donorId, noteId);
        return ResponseEntity.noContent().build();
    }

    // ---- Tags -----------------------------------------------------------

    @GetMapping("/{donorId}/tags")
    @PreAuthorize("hasAuthority('donors.read')")
    public List<DonorTagResponse> listDonorTags(@PathVariable UUID donorId) {
        return tagService.listDonorTags(donorId);
    }

    @PostMapping("/{donorId}/tags/{tagId}")
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<?> assignTag(@PathVariable UUID donorId, @PathVariable UUID tagId) {
        tagService.assignTag(donorId, tagId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{donorId}/tags/{tagId}")
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<?> unassignTag(@PathVariable UUID donorId, @PathVariable UUID tagId) {
        tagService.unassignTag(donorId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<DonorResponse> create(@Valid @RequestBody DonorRequest request) {
        return ResponseEntity.ok(DonorResponse.from(donorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('donors.write')")
    public DonorResponse update(@PathVariable UUID id, @Valid @RequestBody DonorRequest request) {
        return DonorResponse.from(donorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('donors.delete')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        donorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Assignments ----------------------------------------------------

    @GetMapping("/{donorId}/assignments")
    @PreAuthorize("hasAuthority('donors.read')")
    public List<AssignmentResponse> listAssignments(@PathVariable UUID donorId) {
        return assignmentService.listForDonor(donorId);
    }

    @PostMapping("/{donorId}/assignments")
    @PreAuthorize("hasAuthority('donors.assign')")
    public ResponseEntity<AssignmentResponse> assign(@PathVariable UUID donorId,
                                                     @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.assign(donorId, request));
    }

    @DeleteMapping("/{donorId}/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('donors.assign')")
    public ResponseEntity<?> unassign(@PathVariable UUID donorId, @PathVariable UUID assignmentId) {
        assignmentService.unassign(donorId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
