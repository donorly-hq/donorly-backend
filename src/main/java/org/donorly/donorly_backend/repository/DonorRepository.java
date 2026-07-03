package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * NOTE: findByAmbassadorId(String) was removed — Donor no longer
 * has an ambassadorId field. That donor <-> ambassador relationship
 * now lives in ambassador_donor_assignments (see the schema), which
 * doesn't have a Java entity yet. Add an AmbassadorDonorAssignment
 * entity + repository when you rebuild the "donors assigned to me"
 * ambassador view.
 */
public interface DonorRepository extends JpaRepository<Donor, UUID> {
}
