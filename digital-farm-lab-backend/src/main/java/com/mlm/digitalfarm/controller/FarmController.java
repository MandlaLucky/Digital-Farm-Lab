package com.mlm.digitalfarm.controller;

import com.mlm.digitalfarm.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Farm;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.repository.FarmRepository;
import com.mlm.digitalfarm.repository.FieldRepository;

/**
 * Read-only farm API. Ownership is enforced in the repository:
 * if the caller does not own the farm, the lookup returns empty and we
 * answer {@code 404} — the same response as "does not exist", so callers
 * cannot probe for the existence of other users' farms.
 */
@RestController
@RequestMapping("/api/v1/farms")
public class FarmController {

    private final FarmRepository farms;
    private final FieldRepository fields;

    public FarmController(FarmRepository farms, FieldRepository fields) {
        this.farms = farms;
        this.fields = fields;
    }

    @GetMapping
    public List<FarmDto> list(Authentication auth) {
        UUID ownerId = CurrentUser.id(auth);
        return farms.findByOwnerIdOrderByNameAsc(ownerId)
                .stream()
                .map(FarmDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public FarmDto get(@PathVariable UUID id, Authentication auth) {
        UUID ownerId = CurrentUser.id(auth);
        Farm farm = farms.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NoSuchElementException("Farm not found: " + id));
        return FarmDto.from(farm);
    }

    @GetMapping("/{id}/fields")
    public List<FieldSummaryDto> fields(@PathVariable UUID id, Authentication auth) {
        UUID ownerId = CurrentUser.id(auth);
        // One query, one owner-scope: verifies farm ownership and lists fields.
        List<Field> found = fields.findByFarm_IdAndFarm_OwnerIdOrderByNameAsc(id, ownerId);
        // If the farm exists but has no fields, we return []. If the farm is not
        // the caller's, the query also returns [] — but callers should GET /farms/{id}
        // first to distinguish 404 from empty.
        return found.stream().map(FieldSummaryDto::from).toList();
    }

    // ------------------------------------------------------------------
    // DTOs
    // ------------------------------------------------------------------

    public record FarmDto(
            UUID id,
            String name,
            String province,
            BigDecimal totalHa,
            Instant createdAt
    ) {
        static FarmDto from(Farm farm) {
            return new FarmDto(
                    farm.getId(),
                    farm.getName(),
                    farm.getProvince(),
                    farm.getTotalHa(),
                    farm.getCreatedAt());
        }
    }

    public record FieldSummaryDto(
            UUID id,
            String name,
            BigDecimal areaHa,
            String soilType,
            String cropCode,
            LocalDate plantingDate
    ) {
        static FieldSummaryDto from(Field field) {
            return new FieldSummaryDto(
                    field.getId(),
                    field.getName(),
                    field.getAreaHa(),
                    field.getSoilType(),
                    field.getCropCode(),
                    field.getPlantingDate());
        }
    }
}