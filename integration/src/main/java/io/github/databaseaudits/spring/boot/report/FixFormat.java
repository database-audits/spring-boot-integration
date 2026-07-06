package io.github.databaseaudits.spring.boot.report;

import java.util.Locale;

/**
 * The remediation format the report emits for each finding, selected by the
 * {@code database-audits.report.fix-format} configuration parameter. Each
 * constant's {@link #id()} is its external (parameter) name; {@code liquibase-xml}
 * is spelled out so future Liquibase formats (e.g. YAML) can be added alongside
 * it without ambiguity.
 */
public enum FixFormat {
    /** Emit no fixes — the report lists findings only. */
    NONE("none"),

    /** Emit raw SQL DDL. */
    SQL("sql"),

    /** Emit a Liquibase XML {@code databaseChangeLog} fragment. */
    LIQUIBASE_XML("liquibase-xml");

    private final String id;

    FixFormat(final String id) {
        this.id = id;
    }

    /**
     * Returns this format's external name — the value the
     * {@code database-audits.report.fix-format} parameter takes and the report
     * labels a fix section with.
     *
     * @return the format id.
     */
    public String id() {
        return id;
    }

    /**
     * Parses a fix-format id, case-insensitively, falling back to {@link #NONE}
     * for a {@code null}, blank, or unrecognized value.
     *
     * @param value
     *                  the configured fix-format id.
     * @return the matching fix format, or {@link #NONE}.
     */
    public static FixFormat from(final String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        final String candidate = value.strip().toLowerCase(Locale.ROOT);
        for (final FixFormat format : values()) {
            if (format.id.equals(candidate)) {
                return format;
            }
        }
        return NONE;
    }
}
