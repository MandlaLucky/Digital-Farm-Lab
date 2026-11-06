package com.mlm.digitalfarm.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Farm;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Scenario;

public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {

    /**
     * Every scenario created by a user, with its field eagerly loaded so the
     * controller can map to DTOs without a lazy-load exception.
     */
    @EntityGraph(attributePaths = {"field", "field.farm"})
    List<Scenario> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);

    /**
     * Owner-scoped single lookup: the caller must be the creator AND the
     * field's farm must belong to the same user.
     */
    @EntityGraph(attributePaths = {"field", "field.farm"})
    Optional<Scenario> findByIdAndCreatedByAndField_Farm_OwnerId(
            UUID id, UUID createdBy, UUID ownerId);
}