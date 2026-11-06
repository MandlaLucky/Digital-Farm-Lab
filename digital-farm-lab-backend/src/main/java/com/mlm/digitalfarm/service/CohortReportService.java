package com.mlm.digitalfarm.service;

import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.repository.ExerciseRepository;
import com.mlm.digitalfarm.service.RunLookup;
import com.mlm.digitalfarm.dto.scenario.RunSnapshot;
import com.mlm.digitalfarm.service.ScoringService;
import com.mlm.digitalfarm.entity.Submission;
import com.mlm.digitalfarm.repository.SubmissionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Read-only aggregation service for the trainer view.
 *
 * <h2>Cohort model</h2>
 * <p>There is no {@code cohorts} table in this MVP. Two natural groupings are
 * used instead, and both are documented in the architecture note as the
 * follow-up if a real {@code cohorts} + {@code cohort_members} pair is
 * added later:</p>
 * <ul>
 *   <li><b>Exercise = cohort scope.</b> A trainer opens one exercise and
 *       sees how the whole class did on it. This matches the A14 brief line
 *       "Trainer view of cohort results".</li>
 *   <li><b>{@code moduleCode} = cohort code.</b> A module typically has
 *       several exercises; the {@code /cohorts/{moduleCode}/report} endpoint
 *       aggregates the whole module.</li>
 * </ul>
 *
 * <h2>Where the numbers come from</h2>
 * <ul>
 *   <li>Stored {@code submission.score} is authoritative for per-submission
 *       figures (it is what the learner saw at the time).</li>
 *   <li>Aggregates (average, pass rates, distribution) use stored scores.</li>
 *   <li>Per-criterion breakdown re-scores each submission on demand, so it
 *       reflects the <em>current</em> rubric. If that produces a different
 *       score than what was stored, {@code rubricDriftCount} is
 *       incremented and surfaced — honest, cheap, and avoids a schema
 *       change.</li>
 *   <li>Re-scoring is N+1 on {@code runLookup}. Acceptable at class size;
 *       production would store the breakdown at submission time.</li>
 * </ul>
 *
 * <h2>Access model</h2>
 * <p>Any trainer sees every learner's work. Real deployments would scope
 * this to the trainer's own cohort; that requires the cohorts table, which
 * is out of scope for this MVP (recorded in {@code AGENTS.md}).</p>
 */
@Service
@Transactional(readOnly = true)
public class CohortReportService {

    private static final Logger log = LoggerFactory.getLogger(CohortReportService.class);

    /** Max number of recent submissions embedded in a report. */
    private static final int RECENT_LIMIT = 20;

    /** Score buckets for the distribution chart, in points. */
    private static final int BUCKET_WIDTH = 10;

    private final ExerciseRepository exercises;
    private final SubmissionRepository submissions;
    private final ScoringService scoringService;
    private final RunLookup runLookup;

    @PersistenceContext
    private EntityManager entityManager;

    public CohortReportService(ExerciseRepository exercises,
                               SubmissionRepository submissions,
                               ScoringService scoringService,
                               RunLookup runLookup) {
        this.exercises = exercises;
        this.submissions = submissions;
        this.scoringService = scoringService;
        this.runLookup = runLookup;
    }

    // ------------------------------------------------------------------
    // Overview
    // ------------------------------------------------------------------

    public List<ExerciseOverview> overview() {
        return exercises.findAllByOrderByModuleCodeAscTitleAsc().stream()
                .map(this::overviewFor)
                .toList();
    }

    private ExerciseOverview overviewFor(Exercise exercise) {
        List<Submission> all = submissions.findByExerciseIdOrderByCreatedAtDesc(exercise.getId());
        Statistics base = computeStatistics(exercise, all, false);
        return new ExerciseOverview(
                exercise.getId(),
                exercise.getModuleCode(),
                exercise.getTitle(),
                exercise.getPassScore(),
                base.submissionCount(),
                base.uniqueLearners(),
                base.averageScore(),
                base.learnerPassRate());
    }

    // ------------------------------------------------------------------
    // Per-exercise report
    // ------------------------------------------------------------------

    public ExerciseCohortReport reportForExercise(UUID exerciseId) {
        Exercise exercise = exercises.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Exercise not found: " + exerciseId));

        List<Submission> all = submissions.findByExerciseIdOrderByCreatedAtDesc(exerciseId);
        Statistics statistics = computeStatistics(exercise, all, true);
        List<ScoreBucket> distribution = bucketScores(all, exercise.getPassScore());
        List<CriterionStat> criteria = computeCriterionStats(exercise, all);
        List<RecentSubmission> recent = recentWithNames(exercise, RECENT_LIMIT);

        return new ExerciseCohortReport(
                exercise.getId(),
                exercise.getModuleCode(),
                exercise.getTitle(),
                exercise.getPrompt(),
                exercise.getPassScore(),
                statistics,
                distribution,
                criteria,
                recent);
    }

    public List<ExerciseCohortReport> reportForModule(String moduleCode) {
        return exercises.findByModuleCode(moduleCode).stream()
                .map(exercise -> reportForExercise(exercise.getId()))
                .toList();
    }

    // ------------------------------------------------------------------
    // One submission in detail
    // ------------------------------------------------------------------

    public SubmissionDetail submissionDetail(UUID submissionId) {
        Submission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Submission not found: " + submissionId));

        Exercise exercise = submission.getExercise();
        RunSnapshot run = runLookup.findById(submission.getRunId());

        ScoringService.ScoringOutcome rescored = scoringService.score(
                exercise.getRubric(),
                exercise.getPassScore(),
                run,
                submission.getReflection());

        String displayName = lookupDisplayName(submission.getUserId());

        return new SubmissionDetail(
                submission.getId(),
                exercise.getId(),
                submission.getRunId(),
                submission.getUserId(),
                displayName,
                submission.getScore(),
                submission.getScore() >= exercise.getPassScore(),
                exercise.getPassScore(),
                submission.getCreatedAt(),
                submission.getReflection(),
                run.runParams(),
                run.results(),
                run.baseline(),
                rescored.breakdown(),
                rescored.score() != submission.getScore());
    }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    /**
     * @param recomputeDrift when true, re-scores every submission to detect
     *                       rubric drift and fill {@code rubricDriftCount}.
     *                       When false, drift is reported as 0 and no
     *                       {@link RunLookup} calls are made — used by the
     *                       cheap overview endpoint.
     */
    private Statistics computeStatistics(Exercise exercise, List<Submission> all,
                                         boolean recomputeDrift) {
        if (all.isEmpty()) {
            return new Statistics(0, 0, 0, 0,
                    BigDecimal.ZERO, 0, 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        List<Integer> scores = all.stream()
                .map(Submission::getScore)
                .sorted(Comparator.naturalOrder())
                .toList();

        int passThreshold = exercise.getPassScore();
        int passedSubmissions = (int) all.stream()
                .filter(s -> s.getScore() >= passThreshold)
                .count();

        Map<UUID, Boolean> learnerPassed = new LinkedHashMap<>();
        for (Submission s : all) {
            boolean passed = s.getScore() >= passThreshold;
            learnerPassed.merge(s.getUserId(), passed, (a, b) -> a || b);
        }
        int uniqueLearners = learnerPassed.size();
        int passedLearners = (int) learnerPassed.values().stream()
                .filter(Boolean::booleanValue)
                .count();

        BigDecimal averageScore = BigDecimal.valueOf(
                        scores.stream().mapToInt(Integer::intValue).average().orElse(0))
                .setScale(1, RoundingMode.HALF_UP);

        int min = scores.get(0);
        int max = scores.get(scores.size() - 1);
        int median = median(scores);

        BigDecimal submissionPassRate = percentage(passedSubmissions, all.size());
        BigDecimal learnerPassRate = percentage(passedLearners, uniqueLearners);

        int driftCount = recomputeDrift ? countDrift(exercise, all) : 0;

        return new Statistics(
                all.size(),
                uniqueLearners,
                passedSubmissions,
                passedLearners,
                averageScore,
                min,
                median,
                max,
                submissionPassRate,
                learnerPassRate,
                driftCount);
    }

    private int median(List<Integer> sortedScores) {
        int n = sortedScores.size();
        if (n % 2 == 1) {
            return sortedScores.get(n / 2);
        }
        int a = sortedScores.get(n / 2 - 1);
        int b = sortedScores.get(n / 2);
        return Math.round((a + b) / 2.0f);
    }

    private BigDecimal percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(100L * numerator / denominator)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private int countDrift(Exercise exercise, List<Submission> all) {
        int drift = 0;
        for (Submission s : all) {
            try {
                RunSnapshot run = runLookup.findById(s.getRunId());
                int rescored = scoringService.score(
                        exercise.getRubric(),
                        exercise.getPassScore(),
                        run,
                        s.getReflection()).score();
                if (rescored != s.getScore()) {
                    drift++;
                }
            } catch (RuntimeException ex) {
                // Orphaned run or malformed rubric — cannot decide; skip.
                log.debug("Skipped drift check for submission {}: {}",
                        s.getId(), ex.getMessage());
            }
        }
        return drift;
    }

    // ------------------------------------------------------------------
    // Distribution
    // ------------------------------------------------------------------

    private List<ScoreBucket> bucketScores(List<Submission> all, int passScore) {
        int bucketCount = 100 / BUCKET_WIDTH + 1; // 0..9, 10..19, ... 90..100
        int[] counts = new int[bucketCount];

        for (Submission s : all) {
            int score = Math.max(0, Math.min(100, s.getScore()));
            int idx = Math.min(bucketCount - 1, score / BUCKET_WIDTH);
            counts[idx]++;
        }

        List<ScoreBucket> out = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            int lower = i * BUCKET_WIDTH;
            int upper = (i == bucketCount - 1) ? 100 : lower + BUCKET_WIDTH - 1;
            out.add(new ScoreBucket(
                    lower + "-" + upper,
                    lower,
                    upper,
                    lower >= passScore,
                    counts[i]));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Criterion aggregation (re-scores each submission)
    // ------------------------------------------------------------------

    private List<CriterionStat> computeCriterionStats(Exercise exercise, List<Submission> all) {
        if (all.isEmpty()) {
            return List.of();
        }
        Map<String, CriterionAgg> agg = new LinkedHashMap<>();

        for (Submission s : all) {
            RunSnapshot run;
            try {
                run = runLookup.findById(s.getRunId());
            } catch (RuntimeException ex) {
                log.debug("Skipped criterion tally for submission {}: {}",
                        s.getId(), ex.getMessage());
                continue;
            }
            ScoringService.ScoringOutcome outcome = scoringService.score(
                    exercise.getRubric(),
                    exercise.getPassScore(),
                    run,
                    s.getReflection());
            for (ScoringService.CriterionResult criterion : outcome.breakdown()) {
                CriterionAgg a = agg.computeIfAbsent(criterion.key(),
                        k -> new CriterionAgg(criterion.key(), criterion.label(),
                                criterion.weight()));
                a.total++;
                if (criterion.satisfied()) {
                    a.satisfied++;
                }
            }
        }

        return agg.values().stream()
                .map(CriterionAgg::toStat)
                .toList();
    }

    // ------------------------------------------------------------------
    // Recent submissions with display names (native query)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<RecentSubmission> recentWithNames(Exercise exercise, int limit) {
        List<Object[]> rows = entityManager
                .createNativeQuery("""
                        SELECT s.id, s.user_id, u.display_name, s.score, s.created_at
                        FROM submissions s
                        LEFT JOIN users u ON u.id = s.user_id
                        WHERE s.exercise_id = :exerciseId
                        ORDER BY s.created_at DESC
                        LIMIT :limit
                        """)
                .setParameter("exerciseId", exercise.getId())
                .setParameter("limit", limit)
                .getResultList();

        int passThreshold = exercise.getPassScore();
        List<RecentSubmission> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            UUID submissionId = (UUID) row[0];
            UUID userId = (UUID) row[1];
            String displayName = row[2] == null ? null : row[2].toString();
            int score = ((Number) row[3]).intValue();
            Instant createdAt = toInstant(row[4]);
            out.add(new RecentSubmission(
                    submissionId, userId, displayName,
                    score, score >= passThreshold, createdAt));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private String lookupDisplayName(UUID userId) {
        if (userId == null) {
            return null;
        }
        Object result = entityManager
                .createNativeQuery("SELECT display_name FROM users WHERE id = :id")
                .setParameter("id", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);
        return result == null ? null : result.toString();
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        throw new IllegalStateException(
                "Unexpected timestamp type from native query: " + value.getClass());
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private static final class CriterionAgg {
        final String key;
        final String label;
        final int weight;
        int total;
        int satisfied;

        CriterionAgg(String key, String label, int weight) {
            this.key = key;
            this.label = label;
            this.weight = weight;
        }

        CriterionStat toStat() {
            BigDecimal rate = total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(100L * satisfied / total)
                    .setScale(1, RoundingMode.HALF_UP);
            return new CriterionStat(key, label, weight, total, satisfied, rate);
        }
    }

    // ==================================================================
    // Public response records
    // ==================================================================

    /** Quick card shown in the trainer's exercise list. */
    public record ExerciseOverview(
            UUID exerciseId,
            String moduleCode,
            String title,
            int passScore,
            int submissionCount,
            int uniqueLearners,
            BigDecimal averageScore,
            BigDecimal learnerPassRate
    ) { }

    /** Full report for one exercise. */
    public record ExerciseCohortReport(
            UUID exerciseId,
            String moduleCode,
            String title,
            String prompt,
            int passScore,
            Statistics statistics,
            List<ScoreBucket> distribution,
            List<CriterionStat> criteria,
            List<RecentSubmission> recentSubmissions
    ) { }

    /**
     * Aggregate statistics.
     *
     * <p>Two pass rates are exposed deliberately:
     * {@link #submissionPassRate()} counts submissions that passed
     * (a learner who resubmits three times skews this); {@link #learnerPassRate()}
     * counts learners who passed <em>at least once</em> (the useful number
     * for a trainer). The UI should show the learner rate.</p>
     *
     * @param rubricDriftCount number of submissions whose stored score would
     *                         change if re-scored against the current rubric
     */
    public record Statistics(
            int submissionCount,
            int uniqueLearners,
            int passedSubmissions,
            int passedLearners,
            BigDecimal averageScore,
            int minScore,
            int medianScore,
            int maxScore,
            BigDecimal submissionPassRate,
            BigDecimal learnerPassRate,
            int rubricDriftCount
    ) { }

    /** One bar in the score distribution chart. */
    public record ScoreBucket(
            String range,
            int lowerBound,
            int upperBound,
            boolean meetsPassThreshold,
            int count
    ) { }

    /** Per-criterion satisfaction across the cohort. */
    public record CriterionStat(
            String key,
            String label,
            int weight,
            int totalEvaluations,
            int satisfied,
            BigDecimal satisfactionRate
    ) { }

    /** One row in the trainer's recent-submissions table. */
    public record RecentSubmission(
            UUID submissionId,
            UUID userId,
            String displayName,
            int score,
            boolean passed,
            Instant createdAt
    ) { }

    /** Full detail on one submission, with re-scored breakdown. */
    public record SubmissionDetail(
            UUID submissionId,
            UUID exerciseId,
            UUID runId,
            UUID userId,
            String displayName,
            int score,
            boolean passed,
            int passScore,
            Instant createdAt,
            String reflection,
            Map<String, Object> runParams,
            Map<String, Object> results,
            Map<String, Object> baseline,
            List<ScoringService.CriterionResult> breakdown,
            boolean rubricDriftDetected
    ) { }
}