package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Volunteer;
import org.donorly.donorly_backend.repository.VolunteerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;

    public List<Volunteer> getAll() {
        return volunteerRepository.findAll();
    }

    public Optional<Volunteer> getById(String id) {
        return volunteerRepository.findById(id);
    }

    public List<Volunteer> getByAmbassadorId(String ambassadorId) {
        return volunteerRepository.findByAmbassadorId(ambassadorId);
    }

    public Volunteer save(Volunteer volunteer) {
        return volunteerRepository.save(volunteer);
    }

    public void delete(String id) {
        volunteerRepository.deleteById(id);
    }
}
