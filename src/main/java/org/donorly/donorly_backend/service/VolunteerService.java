package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.Volunteer;
import org.donorly.donorly_backend.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VolunteerService {

    @Autowired
    private VolunteerRepository volunteerRepository;

    public List<Volunteer> getAllVolunteers() {
        return volunteerRepository.findAll();
    }

    public Optional<Volunteer> getVolunteerById(String id) {
        return volunteerRepository.findById(id);
    }

    public List<Volunteer> getVolunteersByRole(String role) {
        return volunteerRepository.findByRole(role);
    }

    public Volunteer createVolunteer(Volunteer volunteer) {
        volunteer.setCreatedAt(LocalDateTime.now());
        return volunteerRepository.save(volunteer);
    }

    public Volunteer updateVolunteer(String id, Volunteer updated) {
        updated.setId(id);
        return volunteerRepository.save(updated);
    }

    public void deleteVolunteer(String id) {
        volunteerRepository.deleteById(id);
    }
}
