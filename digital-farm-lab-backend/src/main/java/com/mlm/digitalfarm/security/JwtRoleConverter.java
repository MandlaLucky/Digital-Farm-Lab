package com.mlm.digitalfarm.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Turns a JWT into Spring Security authorities.
 *
 * <p>Recognised claims, in this order:</p>
 * <ul>
 *   <li>{@code roles} — string array or space-separated string</li>
 *   <li>{@code scope} — space-separated string (OAuth2 convention)</li>
 *   <li>{@code scp}   — same as scope, used by Azure AD</li>
 * </ul>
 *
 * <p>Every value is uppercased and prefixed with {@code ROLE_} once, so
 * {@code hasRole('LEARNER')} works regardless of whether the issuer emits
 * {@code learner}, {@code LEARNER}, or {@code ROLE_LEARNER}.</p>
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRolesFromClaim(authorities, jwt.getClaim("roles"));
        addRolesFromClaim(authorities, jwt.getClaim("scope"));
        addRolesFromClaim(authorities, jwt.getClaim("scp"));

        return authorities;
    }

    private void addRolesFromClaim(Set<GrantedAuthority> out, Object claim) {
        if (claim == null) {
            return;
        }
        if (claim instanceof String text) {
            for (String part : text.trim().split("\\s+")) {
                if (!part.isBlank()) {
                    out.add(authority(part));
                }
            }
        } else if (claim instanceof Collection<?> values) {
            for (Object value : values) {
                if (value != null && !value.toString().isBlank()) {
                    out.add(authority(value.toString()));
                }
            }
        } else {
            // Unknown shape; be defensive rather than fail the whole request.
            out.add(authority(claim.toString()));
        }
    }

    private GrantedAuthority authority(String value) {
        String normalised = value.trim().toUpperCase();
        if (!normalised.startsWith(ROLE_PREFIX)) {
            normalised = ROLE_PREFIX + normalised;
        }
        return new SimpleGrantedAuthority(normalised);
    }
}