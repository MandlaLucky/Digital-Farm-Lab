package com.mlm.digitalfarm.controller;

import com.mlm.digitalfarm.dto.scenario.ScenarioParams;
import com.mlm.digitalfarm.dto.scenario.ScenarioResult;
import com.mlm.digitalfarm.entity.Farm;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Scenario;
import com.mlm.digitalfarm.entity.ScenarioRun;
import com.mlm.digitalfarm.repository.FarmRepository;
import com.mlm.digitalfarm.repository.FieldRepository;
import com.mlm.digitalfarm.repository.ScenarioRepository;
import com.mlm.digitalfarm.repository.ScenarioRunRepository;
import com.mlm.digitalfarm.security.CurrentUser;
import com.mlm.digitalfarm.service.ScenarioEngine;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Frontend-facing REST API: shapes match the Vite React app exactly.
 * Stores the "presentation" fields as JSONB metadata while keeping the
 * engine's numeric columns for computation.
 */
@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final FarmRepository farms;
    private final FieldRepository fields;
    private final ScenarioRepository scenarios;
    private final ScenarioRunRepository runs;
    private final ScenarioEngine engine;

    public WorkspaceController(FarmRepository farms, FieldRepository fields,
                               ScenarioRepository scenarios, ScenarioRunRepository runs,
                               ScenarioEngine engine) {
        this.farms = farms;
        this.fields = fields;
        this.scenarios = scenarios;
        this.runs = runs;
        this.engine = engine;
    }

    // ─── FARMS ──────────────────────────────────────────────────────────

    @GetMapping("/farms")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFarms(Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return farms.findByOwnerIdOrderByNameAsc(userId).stream().map(this::farmToJson).toList();
    }

    @PostMapping("/farms")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createFarm(@RequestBody Map<String, Object> body, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        String name = string(body, "name", "Unnamed farm");
        String location = string(body, "location", "Unknown");
        double totalSize = number(body, "totalSize", 40).doubleValue();

        Farm farm = new Farm(userId, name, location, BigDecimal.valueOf(totalSize));
        farm.setMetadata(body);
        Farm saved = farms.save(farm);

        // Create Field entities for engine compatibility
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldList = (List<Map<String, Object>>) body.getOrDefault("fields", List.of());
        for (Map<String, Object> f : fieldList) {
            String fName = string(f, "name", "Field A");
            double fSize = number(f, "size", 5).doubleValue();
            String soil = string(f, "soil", "loamy");
            String crop = string(f, "crop", "none");
            Field field = new Field(saved, fName, BigDecimal.valueOf(fSize),
                    soil, crop.toUpperCase(), LocalDate.now());
            fields.save(field);
        }

        return farmToJson(saved);
    }

    @PutMapping("/farms/{id}")
    @Transactional
    public Map<String, Object> updateFarm(@PathVariable UUID id, @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        Farm farm = farms.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        farm.setName(string(body, "name", farm.getName()));
        farm.setMetadata(body);
        return farmToJson(farms.save(farm));
    }

    @DeleteMapping("/farms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteFarm(@PathVariable UUID id, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        farms.findByIdAndOwnerId(id, userId).ifPresent(farms::delete);
    }

    // ─── SCENARIOS ──────────────────────────────────────────────────────

    @GetMapping("/scenarios")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listScenarios(Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return scenarios.findByCreatedByOrderByCreatedAtDesc(userId).stream()
                .map(this::scenarioToJson).toList();
    }

    @PostMapping("/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createScenario(@RequestBody Map<String, Object> body, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        String name = string(body, "name", "Unnamed scenario");
        Field field = getOrCreateFieldForUser(userId);

        Map<String, Object> engineParams = new LinkedHashMap<>();
        engineParams.put("irrigationMm", 440);
        engineParams.put("fertiliserKgHa", 180);
        engineParams.put("plantingDate", "2026-10-15");
        engineParams.put("irrigationReductionPct", 20);

        Scenario scenario = new Scenario(field, userId, name, engineParams);
        scenario.setMetadata(body);
        return scenarioToJson(scenarios.save(scenario));
    }

    @PutMapping("/scenarios/{id}")
    @Transactional
    public Map<String, Object> updateScenario(@PathVariable UUID id, @RequestBody Map<String, Object> body,
                                              Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        Scenario s = scenarios.findByIdAndCreatedByAndField_Farm_OwnerId(id, userId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario not found"));
        s.setName(string(body, "name", s.getName()));
        s.setMetadata(body);
        return scenarioToJson(scenarios.save(s));
    }

    @DeleteMapping("/scenarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteScenario(@PathVariable UUID id, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        scenarios.findByIdAndCreatedByAndField_Farm_OwnerId(id, userId, userId).ifPresent(scenarios::delete);
    }

    // ─── RUNS ───────────────────────────────────────────────────────────

    @GetMapping("/runs")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRuns(Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        return runs.findByRunByOrderByCreatedAtDesc(userId).stream()
                .map(this::runToJson).toList();
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createRun(@RequestBody Map<String, Object> body, Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        String scenarioIdStr = (String) body.get("scenarioId");

        Scenario scenario;
        if (scenarioIdStr != null && isUuid(scenarioIdStr)) {
            scenario = scenarios.findByIdAndCreatedByAndField_Farm_OwnerId(
                    UUID.fromString(scenarioIdStr), userId, userId).orElse(null);
        } else {
            scenario = null;
        }
        if (scenario == null) {
            Field field = getOrCreateFieldForUser(userId);
            Map<String, Object> defaultParams = new LinkedHashMap<>();
            defaultParams.put("irrigationMm", 440);
            defaultParams.put("fertiliserKgHa", 180);
            defaultParams.put("plantingDate", "2026-10-15");
            defaultParams.put("irrigationReductionPct", 20);
            scenario = scenarios.save(new Scenario(field, userId,
                    string(body, "scenarioName", "Preset run"), defaultParams));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rawParams = (Map<String, Object>) body.getOrDefault("params", Map.of());
        BigDecimal irrigationMm = BigDecimal.valueOf(number(rawParams, "irrigationMm", 440).doubleValue());
        BigDecimal fertiliserKgHa = BigDecimal.valueOf(number(rawParams, "fertiliserKgHa", 180).doubleValue());
        String plantingDateStr = string(rawParams, "plantingDate", "2026-10-15");
        LocalDate plantingDate = LocalDate.parse(plantingDateStr);
        BigDecimal reductionPct = BigDecimal.valueOf(number(rawParams, "irrigationReductionPct", 20).doubleValue());

        ScenarioParams engineParams = new ScenarioParams(irrigationMm, fertiliserKgHa, plantingDate, reductionPct);
        ScenarioParams baselineParams = new ScenarioParams(
                new BigDecimal("550"), new BigDecimal("180"),
                LocalDate.of(2026, 10, 15), BigDecimal.ZERO);

        ScenarioResult result = engine.run(engineParams, baselineParams);
        ScenarioResult baselineResult = engine.run(baselineParams, baselineParams);

        Map<String, Object> paramsMap = new LinkedHashMap<>();
        paramsMap.put("irrigationMm", irrigationMm);
        paramsMap.put("fertiliserKgHa", fertiliserKgHa);
        paramsMap.put("plantingDate", plantingDateStr);
        paramsMap.put("irrigationReductionPct", reductionPct);

        Map<String, Object> resultsMap = new LinkedHashMap<>();
        resultsMap.put("yieldTha", result.yieldTha());
        resultsMap.put("waterUseM3Ha", result.waterUseM3Ha());
        resultsMap.put("costPerHa", result.costPerHa());
        resultsMap.put("grossMarginPerHa", result.grossMarginPerHa());
        resultsMap.put("disclaimer", result.disclaimer());

        Map<String, Object> baselineMap = new LinkedHashMap<>();
        baselineMap.put("yieldTha", baselineResult.yieldTha());
        baselineMap.put("waterUseM3Ha", baselineResult.waterUseM3Ha());
        baselineMap.put("costPerHa", baselineResult.costPerHa());
        baselineMap.put("grossMarginPerHa", baselineResult.grossMarginPerHa());
        baselineMap.put("disclaimer", baselineResult.disclaimer());

        ScenarioRun run = new ScenarioRun(scenario, userId, paramsMap, resultsMap, baselineMap);
        @SuppressWarnings("unchecked")
        Map<String, Object> record = (Map<String, Object>) body.getOrDefault("record", Map.of());
        run.setMetadata(record);
        ScenarioRun saved = runs.save(run);
        return runToJson(saved);
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private Field getOrCreateFieldForUser(UUID userId) {
        List<Farm> userFarms = farms.findByOwnerIdOrderByNameAsc(userId);
        if (userFarms.isEmpty()) {
            Farm defaultFarm = farms.save(new Farm(userId, "Default Farm", "Unknown", BigDecimal.TEN));
            return fields.save(new Field(defaultFarm, "Field A", BigDecimal.valueOf(5),
                    "loamy", "MAIZE", LocalDate.now()));
        }
        Farm farm = userFarms.get(0);
        List<Field> ff = fields.findByFarm_IdAndFarm_OwnerIdOrderByNameAsc(farm.getId(), userId);
        if (ff.isEmpty()) {
            return fields.save(new Field(farm, "Field A", BigDecimal.valueOf(5),
                    "loamy", "MAIZE", LocalDate.now()));
        }
        return ff.get(0);
    }

    private Map<String, Object> farmToJson(Farm f) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (f.getMetadata() != null) out.putAll(f.getMetadata());
        out.put("id", f.getId().toString());
        out.putIfAbsent("name", f.getName());
        out.putIfAbsent("location", f.getProvince());
        out.putIfAbsent("totalSize", f.getTotalHa());
        return out;
    }

    private Map<String, Object> scenarioToJson(Scenario s) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (s.getMetadata() != null) out.putAll(s.getMetadata());
        out.put("id", s.getId().toString());
        out.putIfAbsent("name", s.getName());
        return out;
    }

    private Map<String, Object> runToJson(ScenarioRun r) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (r.getMetadata() != null) out.putAll(r.getMetadata());
        out.put("id", r.getId().toString());
        out.put("scenarioId", r.getScenario().getId().toString());
        out.put("results", r.getResults());
        out.put("baseline", r.getBaseline());
        out.put("createdAt", r.getCreatedAt().toString());
        return out;
    }

    private static String string(Map<String, Object> m, String k, String fallback) {
        Object v = m.get(k);
        return v == null ? fallback : String.valueOf(v);
    }

    private static Number number(Map<String, Object> m, String k, Number fallback) {
        Object v = m.get(k);
        if (v instanceof Number n) return n;
        if (v instanceof String s) { try { return Double.parseDouble(s); } catch (Exception ignored) {} }
        return fallback;
    }

    private static boolean isUuid(String s) {
        try { UUID.fromString(s); return true; } catch (Exception e) { return false; }
    }
}