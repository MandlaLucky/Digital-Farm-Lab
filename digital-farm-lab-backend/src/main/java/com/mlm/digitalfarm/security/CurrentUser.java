package com.mlm.digitalfarm.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Helpers to read the currently authenticated principal from a request.
 * The principal is expected to be a JWT issued by A1 MyAgriSETA / the Platform Kit,
 * with {@code sub} holding the user's UUID and {@code roles} holding one of
 * {@code LEARNER}, {@code TRAINER}, {@code ADMIN}.
 *
 * <p>Never pass a raw Authentication object into a repository. Always pass the
 * {@link #id(Authentication)} result so tests can stub it cleanly.</p>
 */
public final class CurrentUser {

    private static final String ROLE_PREFIX = "ROLE_";

    private CurrentUser() {
        // utility
    }

    /**
     * @return the authenticated user's UUID, taken from the JWT {@code sub}.
     * @throws AuthenticationCredentialsNotFoundException if there is no principal
     *         or the subject is not a UUID.
     */
    public static UUID id(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user in context");
        }
        String subject = subjectOf(authentication);
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationCredentialsNotFoundException(
                    "JWT subject is not a UUID: " + subject);
        }
    }

    /** Safe variant that does not throw when unauthenticated. */
    public static Optional<UUID> idOrEmpty(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(subjectOf(authentication)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /** True when the principal carries the given role (case-insensitive). */
    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || role == null || role.isBlank()) {
            return false;
        }
        String expected = ROLE_PREFIX + role.toUpperCase();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expected.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static String subjectOf(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new AuthenticationCredentialsNotFoundException("JWT has no subject claim");
            }
            return sub;
        }
        // Fallback for tests that install a UsernamePasswordAuthenticationToken directly.
        return authentication.getName();
    }
}