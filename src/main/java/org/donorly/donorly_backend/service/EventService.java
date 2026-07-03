package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Event;
import org.donorly.donorly_backend.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    public Optional<Event> getById(String id) {
        return eventRepository.findById(id);
    }

    public List<Event> getByAmbassadorId(String ambassadorId) {
        return eventRepository.findByAmbassadorId(ambassadorId);
    }

    public Event save(Event event) {
        return eventRepository.save(event);
    }

    public void delete(String id) {
        eventRepository.deleteById(id);
    }
}
