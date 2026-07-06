package io.github.databaseaudits.spring.boot.report;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.platform.engine.ConfigurationParameters;

/**
 * The report listener's configuration, read from the JUnit Platform
 * configuration parameters (populated from {@code junit-platform.properties}),
 * each key falling back to the same-named system property:
 *
 * <ul>
 * <li>{@code database-audits.report.enabled} — {@code true} (default) writes a
 * report when a run has findings; {@code false} disables it.</li>
 * <li>{@code database-audits.report.format} — {@code asciidoc} (default),
 * {@code markdown}, or {@code text}.</li>
 * <li>{@code database-audits.report.fix-format} — {@code liquibase-xml}
 * (default), {@code sql}, or {@code none}.</li>
 * <li>{@code database-audits.report.fix-placement} — {@code both} (default;
 * fixes inline under each audit and in a consolidated section), {@code inline},
 * or {@code section}.</li>
 * <li>{@code database-audits.report.output-file} — the report path; defaults to
 * {@code target/database-audit-report.<ext>} for the chosen format.</li>
 * </ul>
 *
 * @param enabled
 *                       whether a report is written when a run has findings.
 * @param format
 *                       the report output format.
 * @param fixFormat
 *                       the remediation format.
 * @param placement
 *                       where fixes are placed in the report.
 * @param outputFile
 *                       where the report is written.
 */
public record ReportSettings(boolean enabled, ReportFormat format,
        FixFormat fixFormat, FixPlacement placement, Path outputFile) {
    static final String ENABLED_KEY = "database-audits.report.enabled";

    static final String FORMAT_KEY = "database-audits.report.format";

    static final String FIX_FORMAT_KEY = "database-audits.report.fix-format";

    static final String FIX_PLACEMENT_KEY =
            "database-audits.report.fix-placement";

    static final String OUTPUT_FILE_KEY =
            "database-audits.report.output-file";

    private static final String DEFAULT_FILE_STEM =
            "target/database-audit-report";

    /**
     * Reads the settings from the run's configuration parameters, applying the
     * documented defaults for any that are absent or blank.
     *
     * @param parameters
     *                       the run's JUnit Platform configuration parameters.
     * @return the resolved report settings.
     */
    public static ReportSettings from(
            final ConfigurationParameters parameters) {
        final boolean enabled = value(parameters, ENABLED_KEY)
                .map(Boolean::parseBoolean).orElse(true);
        final ReportFormat format = value(parameters, FORMAT_KEY)
                .map(ReportFormat::from).orElse(ReportFormat.ASCIIDOC);
        final FixFormat fixFormat = value(parameters, FIX_FORMAT_KEY)
                .map(FixFormat::from).orElse(FixFormat.LIQUIBASE_XML);
        final FixPlacement placement = value(parameters, FIX_PLACEMENT_KEY)
                .map(FixPlacement::from).orElse(FixPlacement.BOTH);
        final Path outputFile = value(parameters, OUTPUT_FILE_KEY).map(Path::of)
                .orElseGet(() -> Path
                        .of(DEFAULT_FILE_STEM + "." + format.fileExtension()));
        return new ReportSettings(enabled, format, fixFormat, placement,
                outputFile);
    }

    private static Optional<String> value(
            final ConfigurationParameters parameters, final String key) {
        final Optional<String> configured = parameters.get(key)
                .filter(candidate -> !candidate.isBlank());
        if (configured.isPresent()) {
            return configured;
        }
        return Optional.ofNullable(System.getProperty(key))
                .filter(candidate -> !candidate.isBlank());
    }
}
