package com.mlm.digitalfarm.integration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * In-memory {@link PassportClient}. Never throws; always returns a
 * deterministic credential ID so integration tests and screenshots are
 * reproducible.
 *
 * <p>Activation: every profile except {@code real}.</p>
 */
@Component
@Profile("!real")
public class MockPassportClient implements PassportClient {

    private static final Logger log = LoggerFactory.getLogger(MockPassportClient.class);

    @PostConstruct
    void announce() {
        log.warn("PassportClient is MOCK — credential issuance is logged, not persisted. "
                + "Activate --spring.profiles.active=real when A2 is available.");
    }

    @Override
    public CredentialIssueResponse issueCredential(CredentialIssueRequest request) {
        // Deterministic ID: same user + type → same ID. Reproducible for tests.
        String seed = request.userId() + "|" + request.credentialType();
        String credentialId = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
        Instant issuedAt = Instant.now();

        log.info("Passport MOCK issued credentialId={} userId={} type={} payloadKeys={}",
                credentialId,
                request.userId(),
                request.credentialType(),
                request.payload().keySet());

        return new CredentialIssueResponse(credentialId, "MOCK_ISSUED", issuedAt);
    }
}