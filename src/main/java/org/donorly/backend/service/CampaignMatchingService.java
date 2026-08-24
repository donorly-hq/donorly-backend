package org.donorly.backend.service;

import org.donorly.backend.model.Campaign;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fuzzy campaign-name matching shared by AI card scanning and bulk import,
 * where the name comes from handwriting or a spreadsheet cell and rarely
 * matches the stored name exactly.
 */
@Service
public class CampaignMatchingService {

    /** Case-insensitive match: exact, or one name containing the other. Null when nothing fits. */
    public Campaign match(List<Campaign> campaigns, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String needle = name.strip().toLowerCase();
        return campaigns.stream()
                .filter(c -> {
                    String candidate = c.getName().strip().toLowerCase();
                    return candidate.equals(needle)
                            || candidate.contains(needle)
                            || needle.contains(candidate);
                })
                .findFirst()
                .orElse(null);
    }
}
