package com.mlm.digitalfarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /** Learner history across all exercises. */
    List<Submission> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Learner history for one exercise. */
    List<Submission> findByExerciseIdAndUserIdOrderByCreatedAtDesc(UUID exerciseId, UUID userId);

    /** Trainer view for one exercise. */
    List<Submission> findByExerciseIdOrderByCreatedAtDesc(UUID exerciseId);

    /** Used to avoid duplicate submissions for the same run. */
    Optional<Submission> findByExerciseIdAndRunIdAndUserId(UUID exerciseId, UUID runId, UUID userId);
}