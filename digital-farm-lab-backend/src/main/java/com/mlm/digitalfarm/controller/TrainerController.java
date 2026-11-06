package com.mlm.digitalfarm.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.entity.Submission;
import com.mlm.digitalfarm.service.CohortReportService;

/**
 * Trainer-facing API.
 *
 * <p>Thin by design: every method delegates to {@link CohortReportService}.
 * Access is role-based only — a trainer sees every learner's submissions.
 * Per-trainer cohort scoping would need a real {@code cohorts} table; that
 * is documented as out of scope for the MVP in {@code AGENTS.md}.</p>
 *
 * <p>Response DTOs live in {@link CohortReportService} so the same shape is
 * used by both the API and any future server-side rendering, and so tests
 * can assert on them without a controller.</p>
 */
@RestController
@RequestMapping("/api/v1/trainer")
@PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
public class TrainerController {

    private final CohortReportService reportService;

    public TrainerController(CohortReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * All exercises with quick stats. Cheap — no per-submission re-scoring.
     */
    @GetMapping("/exercises")
    public List<CohortReportService.ExerciseOverview> exercises() {
        return reportService.overview();
    }

    /**
     * Full cohort report for one exercise: statistics, distribution,
     * per-criterion breakdown, recent submissions.
     */
    @GetMapping("/exercises/{id}/report")
    public CohortReportService.ExerciseCohortReport exerciseReport(@PathVariable UUID id) {
        return reportService.reportForExercise(id);
    }

    /**
     * Module-wide view: every exercise with the given module code, each with
     * its own report. This is the closest honest analogue to the brief's
     * "cohort code" without a real {@code cohorts} table.
     */
    @GetMapping("/cohorts/{moduleCode}/report")
    public List<CohortReportService.ExerciseCohortReport> cohortReport(
            @PathVariable String moduleCode) {
        return reportService.reportForModule(moduleCode);
    }

    /**
     * Drill-down on a single submission: full run params, results, baseline,
     * reflection and re-scored criterion breakdown.
     */
    @GetMapping("/submissions/{id}")
    public CohortReportService.SubmissionDetail submission(@PathVariable UUID id) {
        return reportService.submissionDetail(id);
    }
}