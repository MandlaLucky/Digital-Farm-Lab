package com.mlm.digitalfarm.exception;

import java.util.List;
import com.mlm.digitalfarm.entity.Field;

/**
 * Standard error body returned by every non-2xx response.
 *
 * <pre>
 * {
 *   "code": "VALIDATION_FAILED",
 *   "message": "Request validation failed",
 *   "fieldErrors": [
 *     { "field": "irrigationMm", "message": "must be less than or equal to 900" }
 *   ]
 * }
 * </pre>
 */
public record ApiError(String code, String message, List<FieldError> fieldErrors) {

    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    /** Error with no field details. */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }

    /** Validation error with per-field details. */
    public static ApiError validation(String message, List<FieldError> fieldErrors) {
        return new ApiError("VALIDATION_FAILED", message, fieldErrors);
    }

    public record FieldError(String field, String message) {
    }
}