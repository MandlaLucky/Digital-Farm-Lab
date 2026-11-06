package com.mlm.digitalfarm.controller;

import com.mlm.digitalfarm.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.SensorReading;
import com.mlm.digitalfarm.repository.FieldRepository;
import com.mlm.digitalfarm.repository.SensorReadingRepository;
import com.mlm.digitalfarm.service.ScenarioEngine;

/**
 * Read-only field API: sensor readings and a preliminary baseline.
 *
 * <p>The {@code /baseline} endpoint currently returns <em>sensor aggregates</em>
 * over the requested window. When the {@code ScenarioEngine} lands, this
 * endpoint will delegate to it and return the modelled baseline (yield, water,
 * cost, gross margin). The response shape is stable either way — additional
 * fields will be added, never removed.</p>
 */
@RestController
@RequestMapping("/api/v1/fields")
public class FieldController {

    /** Hard cap so a learner cannot accidentally pull a year of raw readings. */
    private static final int MAX_READINGS = 500;

    /** Default window when the caller does not pass from/to. */
    private static final long DEFAULT_WINDOW_DAYS = 7L;

    /** Window used for the baseline endpoint's default. */
    private static final long BASELINE_WINDOW_DAYS = 30L;

    private final FieldRepository fields;
    private final SensorReadingRepository sensorReadings;

    public FieldController(FieldRepository fields, SensorReadingRepository sensorReadings) {
        this.fields = fields;
        this.sensorReadings = sensorReadings;
    }

    // ------------------------------------------------------------------
    // Sensor readings
    // ------------------------------------------------------------------

    @GetMapping("/{id}/sensor-readings")
    public SensorReadingsPage sensorReadings(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) SensorReading.Type type,
            Authentication auth) {

        UUID ownerId = CurrentUser.id(auth);
        // Ownership check first; we never touch readings for a field the caller
        // does not own.
        if (!fields.existsByIdAndFarm_OwnerId(id, ownerId)) {
            throw new NoSuchElementException("Field not found: " + id);
        }

        Window window = resolveWindow(from, to, DEFAULT_WINDOW_DAYS);

        List<SensorReading> readings = (type == null)
                ? sensorReadings.findByField_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
                id, window.from(), window.to())
                : sensorReadings.findByField_IdAndTypeAndRecordedAtBetweenOrderByRecordedAtDesc(
                id, type, window.from(), window.to());

        boolean truncated = readings.size() > MAX_READINGS;
        List<SensorReadingDto> dtos = readings.stream()
                .limit(MAX_READINGS)
                .map(SensorReadingDto::from)
                .toList();

        return new SensorReadingsPage(
                id,
                window.from(),
                window.to(),
                type == null ? null : type.name(),
                dtos.size(),
                truncated,
                dtos);
    }

    // ------------------------------------------------------------------
    // Baseline (sensor aggregates; ScenarioEngine will augment this)
    // ------------------------------------------------------------------

    @GetMapping("/{id}/baseline")
    public FieldBaselineDto baseline(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Authentication auth) {

        UUID ownerId = CurrentUser.id(auth);
        Field field = fields.findByIdAndFarm_OwnerId(id, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Field not found: " + id));

        Window window = resolveWindow(from, to, BASELINE_WINDOW_DAYS);

        List<SensorReading> readings =
                sensorReadings.findByField_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
                        id, window.from(), window.to());

        BigDecimal sumRain = BigDecimal.ZERO;
        BigDecimal sumMoisture = BigDecimal.ZERO;
        BigDecimal sumTemp = BigDecimal.ZERO;
        int countMoisture = 0;
        int countRain = 0;
        int countTemp = 0;

        for (SensorReading r : readings) {
            switch (r.getType()) {
                case RAIN_MM -> {
                    sumRain = sumRain.add(r.getValue());
                    countRain++;
                }
                case SOIL_MOISTURE -> {
                    sumMoisture = sumMoisture.add(r.getValue());
                    countMoisture++;
                }
                case TEMP_C -> {
                    sumTemp = sumTemp.add(r.getValue());
                    countTemp++;
                }
            }
        }

        return new FieldBaselineDto(
                field.getId(),
                field.getName(),
                averageOrNull(sumMoisture, countMoisture),
                countRain > 0 ? sumRain : null,
                averageOrNull(sumTemp, countTemp),
                window.from(),
                window.to(),
                readings.size(),
                Instant.now(),
                "Preliminary sensor aggregates. ScenarioEngine baseline will replace "
                        + "these values once the engine lands. NOT agronomic advice.");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Window resolveWindow(Instant from, Instant to, long defaultDays) {
        Instant effTo = (to != null) ? to : Instant.now();
        Instant effFrom = (from != null) ? from : effTo.minus(defaultDays, ChronoUnit.DAYS);
        if (effFrom.isAfter(effTo)) {
            throw new IllegalArgumentException("'from' must be before or equal to 'to'");
        }
        return new Window(effFrom, effTo);
    }

    private BigDecimal averageOrNull(BigDecimal sum, int count) {
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private record Window(Instant from, Instant to) { }

    // ------------------------------------------------------------------
    // DTOs
    // ------------------------------------------------------------------

    public record SensorReadingDto(
            Long id,
            String type,
            BigDecimal value,
            Instant recordedAt
    ) {
        static SensorReadingDto from(SensorReading r) {
            return new SensorReadingDto(
                    r.getId(),
                    r.getType().name(),
                    r.getValue(),
                    r.getRecordedAt());
        }
    }

    public record SensorReadingsPage(
            UUID fieldId,
            Instant from,
            Instant to,
            String type,
            int count,
            boolean truncated,
            List<SensorReadingDto> readings
    ) { }

    public record FieldBaselineDto(
            UUID fieldId,
            String fieldName,
            BigDecimal averageSoilMoisturePct,
            BigDecimal totalRainMm,
            BigDecimal averageTempC,
            Instant from,
            Instant to,
            int sampleCount,
            Instant computedAt,
            String disclaimer
    ) { }
}