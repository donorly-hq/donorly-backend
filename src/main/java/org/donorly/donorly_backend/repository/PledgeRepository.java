package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Pledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PledgeRepository extends JpaRepository<Pledge, UUID> {

    List<Pledge> findByAmbassadorId(UUID ambassadorId);

    // Replaces the old Ambassador.totalPledged stored field — compute
    // it live instead of keeping it in sync manually.
    @Query("SELECT COALESCE(SUM(p.pledgedAmount), 0) FROM Pledge p WHERE p.ambassadorId = :ambassadorId")
    BigDecimal sumPledgedAmountByAmbassadorId(@Param("ambassadorId") UUID ambassadorId);
}
