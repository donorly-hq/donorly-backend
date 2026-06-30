package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.Ambassador;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.AmbassadorRepository;
import org.donorly.donorly_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AmbassadorService {

    @Autowired
    private AmbassadorRepository ambassadorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Ambassador> getAllAmbassadors() {
        return ambassadorRepository.findAll();
    }

    public Optional<Ambassador> getAmbassadorById(String id) {
        return ambassadorRepository.findById(id);
    }

    public Ambassador createAmbassador(Ambassador ambassador) {
        ambassador.setCreatedAt(LocalDateTime.now());
        return ambassadorRepository.save(ambassador);
    }

    public Ambassador updateAmbassador(String id, Ambassador updated) {
        updated.setId(id);
        return ambassadorRepository.save(updated);
    }

    public void deleteAmbassador(String id) {
        ambassadorRepository.deleteById(id);
    }

    // --- Ambassador hierarchy ---

    /**
     * Creates a sub-ambassador under the given parent. ancestorPath is computed
     * automatically from the parent's own ancestorPath + the parent's id, so the
     * full chain (not just one level) is captured at creation time.
     */
    public Ambassador createSubAmbassador(String parentAmbassadorId, Ambassador newAmbassador) {
        Ambassador parent = ambassadorRepository.findById(parentAmbassadorId)
                .orElseThrow(() -> new IllegalArgumentException("Parent ambassador not found: " + parentAmbassadorId));

        List<String> path = new ArrayList<>(parent.getAncestorPath() != null ? parent.getAncestorPath() : List.of());
        path.add(parent.getId());

        newAmbassador.setParentAmbassadorId(parent.getId());
        newAmbassador.setAncestorPath(path);
        newAmbassador.setCreatedAt(LocalDateTime.now());
        if (newAmbassador.getStatus() == null) {
            newAmbassador.setStatus("active");
        }
        return ambassadorRepository.save(newAmbassador);
    }

    /** Root-level ambassadors (no parent) — Admin-only creation path, enforced in the controller. */
    public Ambassador createRootAmbassador(Ambassador newAmbassador) {
        newAmbassador.setParentAmbassadorId(null);
        newAmbassador.setAncestorPath(new ArrayList<>());
        newAmbassador.setCreatedAt(LocalDateTime.now());
        if (newAmbassador.getStatus() == null) {
            newAmbassador.setStatus("active");
        }
        return ambassadorRepository.save(newAmbassador);
    }

    /** Full downline (all descendants, any depth) of the given ambassador. */
    public List<Ambassador> getDownline(String ambassadorId) {
        return ambassadorRepository.findByAncestorPathContains(ambassadorId);
    }

    /** True if `ancestorId` is the given ambassador itself, or an ancestor of it anywhere in the chain. */
    public boolean isSelfOrAncestor(String ancestorId, String ambassadorId) {
        if (ancestorId == null || ambassadorId == null) return false;
        if (ancestorId.equals(ambassadorId)) return true;
        Optional<Ambassador> target = ambassadorRepository.findById(ambassadorId);
        return target.isPresent() && target.get().getAncestorPath() != null
                && target.get().getAncestorPath().contains(ancestorId);
    }

    /**
     * Deactivates an ambassador. Their existing donors/pledges remain attached
     * to them (no automatic reassignment) — read-only enforcement for an
     * inactive ambassador's data happens in the Donor/PledgeCard services.
     */
    public Ambassador deactivate(String ambassadorId, String requestingAmbassadorId, boolean requesterIsAdmin) {
        Ambassador target = ambassadorRepository.findById(ambassadorId)
                .orElseThrow(() -> new IllegalArgumentException("Ambassador not found: " + ambassadorId));

        if (!requesterIsAdmin && !isSelfOrAncestor(requestingAmbassadorId, ambassadorId)) {
            throw new SecurityException("Not authorized to deactivate this ambassador");
        }

        target.setStatus("inactive");
        return ambassadorRepository.save(target);
    }

    /**
     * Full handover: ambassador `fromId` hands their entire position over to
     * ambassador `toId`. Transfers:
     *  - All donors and pledge cards (ambassadorId reassigned)
     *  - From's spot in the hierarchy (parent + ancestorPath) — `to` takes it
     *  - From's totalPledged is added to to's totalPledged
     *  - From's direct children become to's children (and the whole subtree's
     *    ancestorPath is rewritten to reflect to's new position)
     *  - To's own existing subtree (if any) also gets its ancestorPath rewritten,
     *    since to's own position in the tree has changed
     * `from` is left in place but marked inactive, and their login account is
     * locked out (active = false) so they can no longer sign in.
     *
     * Only `from` themself may trigger this (enforced in the controller).
     */
    public Ambassador handoverTo(String fromId, String toId) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot hand over to yourself");
        }

        Ambassador from = ambassadorRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("Ambassador not found: " + fromId));
        Ambassador to = ambassadorRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("Target ambassador not found: " + toId));

        // 1. Reassign all donors and pledge cards from `from` to `to`
        Query reassignQuery = new Query(Criteria.where("ambassadorId").is(fromId));
        Update reassignUpdate = new Update().set("ambassadorId", toId);
        mongoTemplate.updateMulti(reassignQuery, reassignUpdate, "donors");
        mongoTemplate.updateMulti(reassignQuery, reassignUpdate, "pledge_cards");

        // 2. Record `to`'s OLD prefix (for rewriting its existing descendants below)
        List<String> oldToPrefix = new ArrayList<>(to.getAncestorPath() != null ? to.getAncestorPath() : List.of());
        oldToPrefix.add(to.getId());

        // 3. `to` takes `from`'s position: same parent, same ancestorPath
        List<String> fromAncestorPath = from.getAncestorPath() != null ? from.getAncestorPath() : List.of();
        to.setParentAmbassadorId(from.getParentAmbassadorId());
        to.setAncestorPath(new ArrayList<>(fromAncestorPath));
        to.setTotalPledged((to.getTotalPledged() != null ? to.getTotalPledged() : 0)
                + (from.getTotalPledged() != null ? from.getTotalPledged() : 0));
        ambassadorRepository.save(to);

        List<String> newToPrefix = new ArrayList<>(fromAncestorPath);
        newToPrefix.add(to.getId());

        // 4. Rewrite ancestorPath for `to`'s own pre-existing descendants
        //    (their position moves along with `to`)
        List<Ambassador> toDescendants = ambassadorRepository.findByAncestorPathContains(toId);
        for (Ambassador d : toDescendants) {
            rewriteAncestorPrefix(d, oldToPrefix, newToPrefix);
            ambassadorRepository.save(d);
        }

        // 5. Rewrite `from`'s entire former subtree (children, grandchildren, ...)
        //    to sit under `to` instead. Direct children also get parentAmbassadorId updated.
        List<String> oldFromPrefix = new ArrayList<>(fromAncestorPath);
        oldFromPrefix.add(fromId);

        List<Ambassador> fromDescendants = ambassadorRepository.findByAncestorPathContains(fromId);
        for (Ambassador d : fromDescendants) {
            if (fromId.equals(d.getParentAmbassadorId())) {
                d.setParentAmbassadorId(toId);
            }
            rewriteAncestorPrefix(d, oldFromPrefix, newToPrefix);
            ambassadorRepository.save(d);
        }

        // 6. Deactivate `from` — data record stays (audit trail) but locked out
        from.setStatus("inactive");
        ambassadorRepository.save(from);

        userRepository.findByAmbassadorId(fromId).ifPresent(user -> {
            user.setActive(false);
            user.setActiveSessionToken(null); // also force them out of any current session
            userRepository.save(user);
        });

        return ambassadorRepository.findById(toId).orElse(to);
    }

    /** Replaces the leading `oldPrefix` segment of an ambassador's ancestorPath with `newPrefix`. */
    private void rewriteAncestorPrefix(Ambassador ambassador, List<String> oldPrefix, List<String> newPrefix) {
        List<String> path = ambassador.getAncestorPath() != null ? ambassador.getAncestorPath() : List.of();
        int matchLen = oldPrefix.size();
        List<String> remainder = path.size() > matchLen ? path.subList(matchLen, path.size()) : List.of();
        List<String> rebuilt = new ArrayList<>(newPrefix);
        rebuilt.addAll(remainder);
        ambassador.setAncestorPath(rebuilt);
    }
}
