package com.mlm.digitalfarm.controller;

import com.mlm.digitalfarm.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Farm;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.ScenarioRun;
import com.mlm.digitalfarm.repository.FieldRepository;
import com.mlm.digitalfarm.repository.ScenarioRepository;
import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.service.RunService;

/**
 * Learner-facing scenario and run API. Every endpoint is owner-scoped: the
 * caller must be the scenario's creator AND the field's farm's owner.
 */
@RestController
@RequestMapping("/api/v1")
public class ScenarioController {

    private final RunService runService;
    private final ScenarioRepository scenarios;
    private final com.mlm.digitalfarm.repository.FieldRepository fields;

    public ScenarioController(RunService runService,
                              ScenarioRepository scenarios,
                              com.mlm.digitalfarm.repository.FieldRepository fields) {
        this.runService = runService;
        this.scenarios = scenarios;
        this.fields = fields;
    }

    // ------------------------------------------------------------------
    // Scenarios
    // ------------------------------------------------------------------

    @GetMapping("/scenarios")
    @PreAuthorize("hasAnyRole('LEARNER','TRAINER','ADMIN')")
    public List<ScenarioDto> list(Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return runService.listScenarios(userId).stream().map(ScenarioDto::from).toList();
    }

    @GetMapping("/scenarios/{id}")
    @PreAuthorize("hasAnyRole('LEARNER','TRAINER','ADMIN')")
    public ScenarioDto get(@PathVariable UUID id, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return ScenarioDto.from(runService.loadScenario(id, userId));
    }

    @PostMapping("/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LEARNER','TRAINER')")
    public ScenarioDto create(@Valid @RequestBody CreateScenarioRequest request,
                              Authentication auth) {
        UUID userId = CurrentUser.id(auth);

        var field = fields.findByIdAndFarm_OwnerId(request.fieldId(), userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Field not found: " + request.fieldId()));

        ScenarioParams initial = new ScenarioParams(
                request.irrigationMm(),
                request.fertiliserKgHa(),
                request.plantingDate(),
                request.irrigationReductionPct());

        Scenario saved = runService.createScenario(
                field, userId, request.name(), initial);
        return ScenarioDto.from(saved);
    }

    // ------------------------------------------------------------------
    // Runs
    // ------------------------------------------------------------------

    @PostMapping("/scenarios/{id}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LEARNER','TRAINER')")
    public RunDto run(@PathVariable UUID id,
                      @RequestBody(required = false) RunRequest request,
                      Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        ScenarioParams overrides = (request == null)
                ? null
                : new ScenarioParams(
                request.irrigationMm(),
                request.fertiliserKgHa(),
                request.plantingDate(),
                request.irrigationReductionPct());

        ScenarioRun saved = runService.run(id, userId, overrides);
        return RunDto.from(saved);
    }

    @GetMapping("/scenarios/{id}/runs")
    @PreAuthorize("hasAnyRole('LEARNER','TRAINER','ADMIN')")
    public List<RunSummaryDto> listRuns(@PathVariable UUID id, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return runService.listRuns(id, userId).stream().map(RunSummaryDto::from).toList();
    }

    @GetMapping("/scenarios/{id}/runs/{runId}")
    @PreAuthorize("hasAnyRole('LEARNER','TRAINER','ADMIN')")
    public RunDto getRun(@PathVariable UUID id,
                         @PathVariable UUID runId,
                         Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return RunDto.from(runService.loadRun(id, runId, userId));
    }

    // ------------------------------------------------------------------
    // Request DTOs
    // ------------------------------------------------------------------

    public record CreateScenarioRequest(
            @NotNull UUID fieldId,
            @NotBlank @Size(max = 160) String name,
            BigDecimal irrigationMm,
            BigDecimal fertiliserKgHa,
            LocalDate plantingDate,
            BigDecimal irrigationReductionPct
    ) { }

    /** All fields optional — null means "use the scenario's stored value". */
    public record RunRequest(
            BigDecimal irrigationMm,
            BigDecimal fertiliserKgHa,
            LocalDate plantingDate,
            BigDecimal irrigationReductionPct
    ) { }

    // ------------------------------------------------------------------
    // Response DTOs
    // ------------------------------------------------------------------

    public record ScenarioDto(
            UUID id,
            UUID fieldId,
            String fieldName,
            UUID createdBy,
            String name,
            Map<String, Object> params,
            Instant createdAt
    ) {
        static ScenarioDto from(Scenario s) {
            return new ScenarioDto(
                    s.getId(),
                    s.getField().getId(),
                    s.getField().getName(),
                    s.getCreatedBy(),
                    s.getName(),
                    s.getParams(),
                    s.getCreatedAt());
        }
    }

    public record RunDto(
            UUID id,
            UUID scenarioId,
            UUID runBy,
            Map<String, Object> params,
            Map<String, Object> results,
            Map<String, Object> baseline,
            Integer score,
            Instant createdAt
    ) {
        static RunDto from(ScenarioRun r) {
            return new RunDto(
                    r.getId(),
                    r.getScenario().getId(),
                    r.getRunBy(),
                    r.getParams(),
                    r.getResults(),
                    r.getBaseline(),
                    r.getScore(),
                    r.getCreatedAt());
        }
    }

    public record RunSummaryDto(
            UUID id,
            UUID scenarioId,
            Instant createdAt
    ) {
        static RunSummaryDto from(ScenarioRun r) {
            return new RunSummaryDto(r.getId(), r.getScenario().getId(), r.getCreatedAt());
        }
    }
}