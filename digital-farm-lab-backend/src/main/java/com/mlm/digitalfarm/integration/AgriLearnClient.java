package com.mlm.digitalfarm.integration;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Port to the A3 AgriLearn service (module catalogue).
 *
 * <p>Two implementations are provided:</p>
 * <ul>
 *   <li>{@link MockAgriLearnClient} — active on every profile except {@code real}.
 *       Returns fixture modules that match the seeded exercises so the
 *       demo runs end-to-end without A3 being live.</li>
 *   <li>A real HTTP implementation (to be added in the A3 integration week).
 *       Wire it in with {@code @Profile("real")} when A3 publishes its contract.</li>
 * </ul>
 *
 * <p>Returned {@link Module}s are immutable snapshots. The caller must not
 * assume the module code exists upstream — always pass a code that came from
 * this catalogue.</p>
 */
public interface AgriLearnClient {

    /**
     * Look up a single module by code.
     *
     * @throws NoSuchElementException if the code is unknown upstream.
     * @throws IllegalStateException  if the upstream service is unreachable
     *                                or returns an unexpected response.
     */
    Module fetchModule(String moduleCode);

    /**
     * Full catalogue. Order is unspecified; callers sort as they need.
     */
    List<Module> listModules();

    /**
     * Immutable view of an AgriLearn module.
     *
     * @param source {@code "mock"} or {@code "real"} — surfaced in responses
     *               and logs so the demo is never accidentally dishonest.
     */
    record Module(
            String code,
            String title,
            String description,
            List<String> lessons,
            String language,
            String source
    ) {
        public Module {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Module.code is required");
            }
            title = title == null ? "" : title;
            description = description == null ? "" : description;
            lessons = lessons == null ? List.of() : List.copyOf(lessons);
            language = language == null || language.isBlank() ? "en" : language;
            source = source == null ? "unknown" : source;
        }
    }
}