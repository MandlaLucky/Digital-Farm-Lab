package com.mlm.digitalfarm.repository;

import com.mlm.digitalfarm.entity.ScenarioRun;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScenarioRunRepository extends JpaRepository<ScenarioRun, UUID> {

    @EntityGraph(attributePaths = {"scenario", "scenario.field"})
    List<ScenarioRun> findByScenario_IdOrderByCreatedAtDesc(UUID scenarioId);

    @EntityGraph(attributePaths = {"scenario", "scenario.field", "scenario.field.farm"})
    Optional<ScenarioRun> findByIdAndScenario_CreatedByAndScenario_Field_Farm_OwnerId(
            UUID id, UUID createdBy, UUID ownerId);

    @EntityGraph(attributePaths = {"scenario"})
    Optional<ScenarioRun> findByIdAndRunBy(UUID id, UUID runBy);

    @EntityGraph(attributePaths = {"scenario"})
    List<ScenarioRun> findByRunByOrderByCreatedAtDesc(UUID runBy);
}