package com.mlm.digitalfarm.integration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Port to the A2 AgriSkills Passport credential-issuance API.
 *
 * <p>Callers pass a typed {@link CredentialIssueRequest}; the client is
 * responsible for the wire format. This keeps business code free of JSON
 * string concatenation and lets the mock and real clients diverge freely
 * without touching {@code ExerciseController}.</p>
 *
 * <p>Issuance is <strong>best effort</strong>: a failure here must never
 * roll back the caller's own persistence. {@code ExerciseController}
 * catches every {@link RuntimeException} from this interface and logs it.</p>
 */
public interface PassportClient {

    /**
     * Issue a credential.
     *
     * @return the upstream's view of the issued credential (at minimum an ID
     *         and timestamp). Never {@code null}.
     * @throws IllegalStateException on any transport or upstream error.
     */
    CredentialIssueResponse issueCredential(CredentialIssueRequest request);

    /**
     * Typed request payload.
     *
     * @param userId          subject user (JWT {@code sub})
     * @param credentialType  short label, e.g. {@code A14-LEARN-CROP-01}
     * @param payload         additional data (score, module, timestamp, etc.)
     */
    record CredentialIssueRequest(
            UUID userId,
            String credentialType,
            Map<String, Object> payload
    ) {
        public CredentialIssueRequest {
            if (userId == null) {
                throw new IllegalArgumentException("CredentialIssueRequest.userId is required");
            }
            if (credentialType == null || credentialType.isBlank()) {
                throw new IllegalArgumentException(
                        "CredentialIssueRequest.credentialType is required");
            }
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    /**
     * Upstream response.
     *
     * @param credentialId  opaque ID assigned by the upstream (or a
     *                      deterministic mock ID)
     * @param status        {@code ISSUED}, {@code PENDING}, or {@code MOCK_ISSUED}
     * @param issuedAt      timestamp from the upstream (or {@code Instant.now()}
     *                      for mocks)
     */
    record CredentialIssueResponse(
            String credentialId,
            String status,
            Instant issuedAt
    ) { }
}