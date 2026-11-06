package com.mlm.digitalfarm.controller;

import com.mlm.digitalfarm.security.CurrentUser;
import com.mlm.digitalfarm.integration.PassportClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mlm.digitalfarm.integration.PassportClient;
import com.mlm.digitalfarm.integration.PassportClient.CredentialIssueRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.entity.Submission;
import com.mlm.digitalfarm.repository.ExerciseRepository;
import com.mlm.digitalfarm.repository.SubmissionRepository;
import com.mlm.digitalfarm.dto.scenario.RunSnapshot;
import com.mlm.digitalfarm.service.ScoringService;
import com.mlm.digitalfarm.service.RunLookup;

/**
 * Learner-facing exercise API. Submitting an exercise scores the run
 * against the rubric and, on pass, fires an A2 Passport credential event.
 *
 * <p>Ownership rule: a learner may only submit their own runs. Trying to
 * submit someone else's {@code runId} returns 403.</p>
 */
@RestController
@RequestMapping("/api/v1/exercises")
public class ExerciseController {

    private static final Logger log = LoggerFactory.getLogger(ExerciseController.class);

    private final ExerciseRepository exercises;
    private final SubmissionRepository submissions;
    private final ScoringService scoringService;
    private final RunLookup runLookup;
    private final PassportClient passportClient;

    public ExerciseController(ExerciseRepository exercises,
                              SubmissionRepository submissions,
                              ScoringService scoringService,
                              RunLookup runLookup,
                              PassportClient passportClient) {
        this.exercises = exercises;
        this.submissions = submissions;
        this.scoringService = scoringService;
        this.runLookup = runLookup;
        this.passportClient = passportClient;
    }

    // ------------------------------------------------------------------
    // Catalogue
    // ------------------------------------------------------------------

    @GetMapping
    public List<ExerciseSummary> list(@RequestParam(required = false) String moduleCode) {
        List<Exercise> found = (moduleCode == null || moduleCode.isBlank())
                ? exercises.findAllByOrderByModuleCodeAscTitleAsc()
                : exercises.findByModuleCode(moduleCode);
        return found.stream().map(this::toSummary).toList();
    }

    @GetMapping("/{id}")
    public ExerciseDetail get(@PathVariable UUID id) {
        Exercise exercise = exercises.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Exercise not found: " + id));
        return toDetail(exercise);
    }

    // ------------------------------------------------------------------
    // Submissions
    // ------------------------------------------------------------------

    @PostMapping("/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LEARNER')")
    public SubmitResponse submit(@PathVariable UUID id,
                                 @Valid @RequestBody SubmitRequest request,
                                 Authentication authentication) {

        UUID userId = CurrentUser.id(authentication);

        Exercise exercise = exercises.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Exercise not found: " + id));

        RunSnapshot run = runLookup.findById(request.runId());
        if (!userId.equals(run.runBy())) {
            throw new AccessDeniedException("You can only submit your own runs.");
        }

        // Reject duplicate submissions of the same run for the same exercise.
        submissions.findByExerciseIdAndRunIdAndUserId(id, request.runId(), userId)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "This run has already been submitted for this exercise (submission "
                                    + existing.getId() + ").");
                });

        ScoringService.ScoringOutcome outcome = scoringService.score(
                exercise.getRubric(),
                exercise.getPassScore(),
                run,
                request.reflection());

        Submission submission = submissions.save(new Submission(
                exercise,
                run.runId(),
                userId,
                outcome.score(),
                request.reflection(),
                Instant.now()));

        if (outcome.passed()) {
            issuePassportCredential(exercise, submission);
        }

        return new SubmitResponse(
                submission.getId(),
                submission.getRunId(),
                outcome.score(),
                outcome.passed(),
                outcome.passScore(),
                outcome.breakdown(),
                submission.getCreatedAt());
    }

    @GetMapping("/{id}/submissions/me")
    public List<SubmissionSummary> mySubmissions(@PathVariable UUID id,
                                                 Authentication authentication) {
        UUID userId = CurrentUser.id(authentication);
        if (!exercises.existsById(id)) {
            throw new NoSuchElementException("Exercise not found: " + id);
        }
        return submissions.findByExerciseIdAndUserIdOrderByCreatedAtDesc(id, userId)
                .stream()
                .map(s -> toSummary(s, exercises.getReferenceById(id).getPassScore()))
                .toList();
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasRole('TRAINER')")
    public List<SubmissionSummary> allSubmissions(@PathVariable UUID id) {
        Exercise exercise = exercises.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Exercise not found: " + id));
        return submissions.findByExerciseIdOrderByCreatedAtDesc(id)
                .stream()
                .map(s -> toSummary(s, exercise.getPassScore()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Passport integration (best effort — never fails the submission)
    // ------------------------------------------------------------------

// ------------------------------------------------------------------
// Passport integration (best effort — never fails the submission)
// ------------------------------------------------------------------

    // ------------------------------------------------------------------
// Passport integration (best effort — never fails the submission)
// ------------------------------------------------------------------

    private void issuePassportCredential(Exercise exercise, Submission submission) {
        try {
            CredentialIssueRequest request = new CredentialIssueRequest(
                    submission.getUserId(),
                    "A14-" + exercise.getModuleCode(),
                    Map.of(
                            "exerciseId", exercise.getId().toString(),
                            "moduleCode", exercise.getModuleCode(),
                            "score", submission.getScore(),
                            "passedAt", submission.getCreatedAt().toString()));
            PassportClient.CredentialIssueResponse response =
                    passportClient.issueCredential(request);
            log.info("Passport credential issued: credentialId={} submission={}",
                    response.credentialId(), submission.getId());
        } catch (RuntimeException ex) {
            log.warn("Passport issuance failed for submission {} (submission saved anyway)",
                    submission.getId(), ex);
        }
    }

    private String toJson(Map<String, Object> payload) {
        // Small, dependency-free JSON serializer for flat string/number maps.
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(String.valueOf(value).replace("\"", "\\\"")).append('"');
            }
        }
        return sb.append('}').toString();
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private ExerciseSummary toSummary(Exercise exercise) {
        return new ExerciseSummary(
                exercise.getId(),
                exercise.getModuleCode(),
                exercise.getTitle(),
                exercise.getPrompt(),
                exercise.getPassScore());
    }

    private ExerciseDetail toDetail(Exercise exercise) {
        return new ExerciseDetail(
                exercise.getId(),
                exercise.getModuleCode(),
                exercise.getTitle(),
                exercise.getPrompt(),
                exercise.getRubric(),
                exercise.getPassScore());
    }

    private SubmissionSummary toSummary(Submission submission, int passScore) {
        return new SubmissionSummary(
                submission.getId(),
                submission.getExercise().getId(),
                submission.getRunId(),
                submission.getUserId(),
                submission.getScore(),
                submission.getScore() >= passScore,
                submission.getCreatedAt());
    }

    // ------------------------------------------------------------------
    // DTOs
    // ------------------------------------------------------------------

    public record SubmitRequest(
            @NotNull UUID runId,
            @Size(max = 2000) String reflection
    ) { }

    public record SubmitResponse(
            UUID id,
            UUID runId,
            int score,
            boolean passed,
            int passScore,
            List<ScoringService.CriterionResult> breakdown,
            Instant createdAt
    ) { }

    public record SubmissionSummary(
            UUID id,
            UUID exerciseId,
            UUID runId,
            UUID userId,
            int score,
            boolean passed,
            Instant createdAt
    ) { }

    public record ExerciseSummary(
            UUID id,
            String moduleCode,
            String title,
            String prompt,
            int passScore
    ) { }

    public record ExerciseDetail(
            UUID id,
            String moduleCode,
            String title,
            String prompt,
            Map<String, Object> rubric,
            int passScore
    ) { }
}