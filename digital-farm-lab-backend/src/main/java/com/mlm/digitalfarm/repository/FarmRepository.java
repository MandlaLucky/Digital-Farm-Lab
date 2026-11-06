package com.mlm.digitalfarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Farm;

public interface FarmRepository extends JpaRepository<Farm, UUID> {

    /** All farms owned by a user, name-ordered. */
    List<Farm> findByOwnerIdOrderByNameAsc(UUID ownerId);

    /** Owner-scoped single lookup. Empty means "not found for this user". */
    Optional<Farm> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Convenience for fast ownership checks without loading the entity. */
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}