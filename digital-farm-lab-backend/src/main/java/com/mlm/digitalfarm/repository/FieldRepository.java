package com.mlm.digitalfarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Farm;
import com.mlm.digitalfarm.entity.Field;

public interface FieldRepository extends JpaRepository<Field, UUID> {

    /**
     * Fields of a farm, but only when the farm is owned by {@code ownerId}.
     * Underscores in the derived query disambiguate the two {@code Farm}
     * traversals: {@code farm.id} and {@code farm.ownerId}.
     */
    List<Field> findByFarm_IdAndFarm_OwnerIdOrderByNameAsc(UUID farmId, UUID ownerId);

    /** Owner-scoped single lookup. Empty means "not found for this user". */
    Optional<Field> findByIdAndFarm_OwnerId(UUID id, UUID ownerId);

    /** Fast ownership check without loading the entity. */
    boolean existsByIdAndFarm_OwnerId(UUID id, UUID ownerId);
}