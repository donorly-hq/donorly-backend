package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Ambassador;
import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.model.PledgeCard;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.AmbassadorRepository;
import org.donorly.donorly_backend.repository.DonorRepository;
import org.donorly.donorly_backend.repository.PledgeCardRepository;
import org.donorly.donorly_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AmbassadorService {

    private final AmbassadorRepository ambassadorRepository;
    private final DonorRepository donorRepository;
    private final PledgeCardRepository pledgeCardRepository;
    private final UserRepository userRepository;

    public List<Ambassador> getAll() {
        return ambassadorRepository.findAll();
    }

    public Optional<Ambassador> getById(String id) {
        return ambassadorRepository.findById(id);
    }

    public Ambassador save(Ambassador ambassador) {
        return ambassadorRepository.save(ambassador);
    }

    public Ambassador createRootAmbassador(Ambassador ambassador) {
        ambassador.setAncestorPath(new ArrayList<>());
        ambassador.setParentAmbassadorId(null);
        return ambassadorRepository.save(ambassador);
    }

    public Ambassador createSubAmbassador(String parentId, Ambassador ambassador) {
        Ambassador parent = ambassadorRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent ambassador not found"));

        List<String> ancestorPath = new ArrayList<>(parent.getAncestorPath());
        ancestorPath.add(parentId);

        ambassador.setParentAmbassadorId(parentId);
        ambassador.setAncestorPath(ancestorPath);
        return ambassadorRepository.save(ambassador);
    }

    public List<Ambassador> getDownline(String ambassadorId) {
        return ambassadorRepository.findByAncestorPathContains(ambassadorId);
    }

    public boolean isSelfOrAncestor(String ambassadorId, String targetId) {
        if (ambassadorId.equals(targetId)) return true;
        return ambassadorRepository.findById(targetId)
                .map(a -> a.getAncestorPath().contains(ambassadorId))
                .orElse(false);
    }

    public Ambassador deactivate(String id) {
        Ambassador ambassador = ambassadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ambassador not found"));
        ambassador.setStatus("Inactive");
        return ambassadorRepository.save(ambassador);
    }

    @Transactional
    public void handoverTo(String fromId, String toId) {
        Ambassador from = ambassadorRepository.findById(fromId)
                .orElseThrow(() -> new RuntimeException("From ambassador not found"));
        Ambassador to = ambassadorRepository.findById(toId)
                .orElseThrow(() -> new RuntimeException("To ambassador not found"));

        // Transfer donors
        List<Donor> donors = donorRepository.findByAmbassadorId(fromId);
        donors.forEach(d -> d.setAmbassadorId(toId));
        donorRepository.saveAll(donors);

        // Transfer pledge cards
        List<PledgeCard> pledges = pledgeCardRepository.findByAmbassadorId(fromId);
        pledges.forEach(p -> p.setAmbassadorId(toId));
        pledgeCardRepository.saveAll(pledges);

        // Transfer pledge total
        to.setTotalPledged((to.getTotalPledged() == null ? 0.0 : to.getTotalPledged())
                + (from.getTotalPledged() == null ? 0.0 : from.getTotalPledged()));

        // Transfer hierarchy position
        to.setParentAmbassadorId(from.getParentAmbassadorId());
        to.setAncestorPath(new ArrayList<>(from.getAncestorPath()));
        ambassadorRepository.save(to);

        // Update all of 'to's pre-existing descendants' ancestorPath
        List<Ambassador> toDescendants = ambassadorRepository.findByAncestorPathContains(toId);
        toDescendants.forEach(d -> {
            List<String> path = new ArrayList<>(d.getAncestorPath());
            int idx = path.indexOf(fromId);
            if (idx >= 0) path.set(idx, toId);
            d.setAncestorPath(path);
        });
        ambassadorRepository.saveAll(toDescendants);

        // Update from's former subtree
        List<Ambassador> fromDescendants = ambassadorRepository.findByAncestorPathContains(fromId);
        fromDescendants.forEach(d -> {
            List<String> path = new ArrayList<>(d.getAncestorPath());
            path.replaceAll(id -> id.equals(fromId) ? toId : id);
            d.setAncestorPath(path);
            if (fromId.equals(d.getParentAmbassadorId())) {
                d.setParentAmbassadorId(toId);
            }
        });
        ambassadorRepository.saveAll(fromDescendants);

        // Deactivate 'from'
        from.setStatus("Inactive");
        from.setTotalPledged(0.0);
        from.setParentAmbassadorId(null);
        from.setAncestorPath(new ArrayList<>());
        ambassadorRepository.save(from);

        // Lock 'from' user login
        userRepository.findByEmail(from.getEmail()).ifPresent(user -> {
            user.setActive(false);
            user.setActiveSessionToken(null);
            userRepository.save(user);
        });
    }
}
