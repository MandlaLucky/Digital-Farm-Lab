package com.mlm.digitalfarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.SensorReading;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    /**
     * All readings for a field in the window, newest first. The caller is
     * expected to have already verified field ownership; this repository
     * does not repeat that check.
     */
    List<SensorReading> findByField_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
            java.util.UUID fieldId, Instant from, Instant to);

    /** As above, but filtered to one sensor type. */
    List<SensorReading> findByField_IdAndTypeAndRecordedAtBetweenOrderByRecordedAtDesc(
            java.util.UUID fieldId, SensorReading.Type type, Instant from, Instant to);
}