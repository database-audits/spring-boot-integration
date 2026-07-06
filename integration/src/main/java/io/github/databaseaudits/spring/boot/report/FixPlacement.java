package io.github.databaseaudits.spring.boot.report;

import java.util.Locale;

/**
 * Where a report places its per-finding fixes, selected by the
 * {@code database-audits.report.fix-placement} configuration parameter. Inline
 * placement shows each audit's fix directly under its findings (easy to read
 * together); the consolidated section gathers every fix in one block — for
 * {@code liquibase-xml} that section is the single applicable
 * {@code databaseChangeLog}, so {@link #BOTH} keeps the inline readability while
 * still emitting one changelog to apply.
 */
public enum FixPlacement {
    /** A single consolidated fix section at the end of the report. */
    SECTION("section"),

    /** Each audit's fixes inline, directly under its findings. */
    INLINE("inline"),

    /** Both — fixes inline under each audit and in a consolidated section. */
    BOTH("both");

    private final String id;

    FixPlacement(final String id) {
        this.id = id;
    }

    /**
     * Returns this placement's external (configuration-parameter) name.
     *
     * @return the placement id.
     */
    public String id() {
        return id;
    }

    /**
     * Whether fixes are rendered inline under each audit for this placement.
     *
     * @return {@code true} for {@link #INLINE} and {@link #BOTH}.
     */
    boolean inline() {
        return this == INLINE || this == BOTH;
    }

    /**
     * Whether fixes are rendered in the consolidated end section for this
     * placement.
     *
     * @return {@code true} for {@link #SECTION} and {@link #BOTH}.
     */
    boolean section() {
        return this == SECTION || this == BOTH;
    }

    /**
     * Parses a fix-placement id, case-insensitively, falling back to
     * {@link #BOTH} for a {@code null}, blank, or unrecognized value.
     *
     * @param value
     *                  the configured fix-placement id.
     * @return the matching placement, or {@link #BOTH}.
     */
    public static FixPlacement from(final String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }
        final String candidate = value.strip().toLowerCase(Locale.ROOT);
        for (final FixPlacement placement : values()) {
            if (placement.id.equals(candidate)) {
                return placement;
            }
        }
        return BOTH;
    }
}
