package com.mlm.digitalfarm.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mlm.digitalfarm.entity.Field;
import com.mlm.digitalfarm.entity.Exercise;
import com.mlm.digitalfarm.entity.Submission;

/**
 * HTTP implementation of {@link PassportClient} for the A2 AgriSkills
 * Passport service.
 *
 * <p>Activation: {@code --spring.profiles.active=real}.</p>
 *
 * <p>Transport configuration comes from
 * {@code agriconnect.passport.base-url} (env: {@code AGRICONNECT_PASSPORT_BASE_URL}).
 * Timeouts are intentionally short (5 s connect / 10 s read) so a Passport
 * outage cannot stall an exercise submission.</p>
 *
 * <p>Every failure mode — timeout, connection refused, 4xx, 5xx, malformed
 * JSON — is translated into an {@link IllegalStateException}. The caller is
 * expected to log and continue; credential issuance is best effort.</p>
 */
@Component
@Profile("real")
public class RealPassportClient implements PassportClient {

    private static final Logger log = LoggerFactory.getLogger(RealPassportClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String ISSUE_PATH = "/api/v1/credentials";

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public RealPassportClient(
            @Value("${agriconnect.passport.base-url}") String baseUrl,
            ObjectMapper objectMapper) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;

        log.warn("PassportClient is REAL — targeting {}", baseUrl);
    }

    @Override
    public CredentialIssueResponse issueCredential(CredentialIssueRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", request.userId().toString());
        body.put("credentialType", request.credentialType());
        body.put("payload", request.payload());

        try {
            JsonNode response = http.post()
                    .uri(ISSUE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new IllegalStateException(
                                "Passport returned " + res.getStatusCode()
                                        + " for credential type " + request.credentialType());
                    })
                    .body(JsonNode.class);

            return toResponse(request, response);

        } catch (IllegalStateException ex) {
            // Already a well-formed error; re-throw so the caller logs the reason.
            log.warn("Passport issuance failed: userId={} type={} reason={}",
                    request.userId(), request.credentialType(), ex.getMessage());
            throw ex;

        } catch (RuntimeException ex) {
            // Transport-level (connection refused, timeout, …).
            log.warn("Passport transport error: userId={} type={}",
                    request.userId(), request.credentialType(), ex);
            throw new IllegalStateException("Passport unreachable", ex);
        }
    }

    // ------------------------------------------------------------------
    // Response mapping
    // ------------------------------------------------------------------

    private CredentialIssueResponse toResponse(CredentialIssueRequest request, JsonNode response) {
        if (response == null || response.isNull()) {
            // Some services return 201 + empty body. Synthesise a stable ID.
            return new CredentialIssueResponse(
                    UUID.nameUUIDFromBytes(
                                    (request.userId() + "|" + request.credentialType())
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                            .toString(),
                    "ISSUED",
                    Instant.now());
        }

        String credentialId = textOrNull(response, "credentialId");
        if (credentialId == null) {
            credentialId = textOrNull(response, "id");
        }
        if (credentialId == null) {
            throw new IllegalStateException(
                    "Passport response missing credentialId/id: " + truncate(response.toString()));
        }

        String status = textOrNull(response, "status");
        if (status == null || status.isBlank()) {
            status = "ISSUED";
        }

        Instant issuedAt = parseInstant(textOrNull(response, "issuedAt"));
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }

        log.debug("Passport issued credentialId={} status={} for type={}",
                credentialId, status, request.credentialType());

        return new CredentialIssueResponse(credentialId, status, issuedAt);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? null : child.asText();
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (RuntimeException ex) {
            try {
                return java.time.OffsetDateTime.parse(raw).toInstant();
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }

    private String truncate(String value) {
        return value == null || value.length() <= 200 ? value : value.substring(0, 200) + "…";
    }
}