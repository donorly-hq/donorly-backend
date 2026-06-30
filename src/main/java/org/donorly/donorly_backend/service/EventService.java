package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.Event;
import org.donorly.donorly_backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(String id) {
        return eventRepository.findById(id);
    }

    public List<Event> getEventsByStatus(String status) {
        return eventRepository.findByStatus(status);
    }

    public Event createEvent(Event event) {
        event.setCreatedAt(LocalDateTime.now());
        return eventRepository.save(event);
    }

    public Event updateEvent(String id, Event updated) {
        updated.setId(id);
        return eventRepository.save(updated);
    }

    public void deleteEvent(String id) {
        eventRepository.deleteById(id);
    }
}
