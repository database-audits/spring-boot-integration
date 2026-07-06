package io.github.databaseaudits.spring.boot.report;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.databaseaudits.audit.finding.Finding;
import io.github.databaseaudits.spring.boot.assertion.AuditFamily;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditFailure;

/**
 * Assembles the consolidated audit report from the {@link DatabaseAuditFailure}s
 * a run collected. Findings are grouped by {@link AuditFamily} then audit, each
 * audit showing its curated header and the verbatim finding descriptions; when a
 * {@link FixFormat} other than {@link FixFormat#NONE} is selected, each finding's
 * remediation — raw SQL, or a Liquibase change set — is placed inline under its
 * audit, in one consolidated section, or both, per the {@link FixPlacement}. The
 * output uses {@code \n} line breaks so a report is byte-for-byte deterministic
 * across platforms.
 */
final class DatabaseAuditReport {
    private static final String NL = "\n";

    private static final String SECTION_BREAK = NL + NL;

    private static final String LIQUIBASE_HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <databaseChangeLog
                xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">""";

    private DatabaseAuditReport() {
    }

    /**
     * Renders the report for the collected failures.
     *
     * @param failures
     *                      the audit failures collected over the run; must be
     *                      non-empty.
     * @param format
     *                      the report output format.
     * @param fixFormat
     *                      the remediation format, or {@link FixFormat#NONE} to
     *                      omit fixes.
     * @param placement
     *                      where fixes are placed — inline under each audit, in a
     *                      consolidated section, or both.
     * @param fixRenderer
     *                      the renderer that turns findings into fixes.
     * @return the rendered report.
     */
    static String render(final List<DatabaseAuditFailure> failures,
            final ReportFormat format, final FixFormat fixFormat,
            final FixPlacement placement, final FixRenderer fixRenderer) {
        final List<DatabaseAuditFailure> ordered = failures.stream()
                .sorted(Comparator
                        .comparingInt(
                                (final DatabaseAuditFailure f) -> f.family()
                                        .ordinal())
                        .thenComparing(DatabaseAuditFailure::auditName))
                .toList();
        final boolean withFixes = fixFormat != FixFormat.NONE;

        final StringBuilder report = new StringBuilder();
        report.append(format.heading(1, "Database Audit Report"))
                .append(SECTION_BREAK).append(summary(ordered));

        AuditFamily currentFamily = null;
        for (final DatabaseAuditFailure failure : ordered) {
            if (failure.family() != currentFamily) {
                currentFamily = failure.family();
                report.append(SECTION_BREAK)
                        .append(format.heading(2, currentFamily.name()));
            }
            report.append(SECTION_BREAK)
                    .append(format.heading(3, failure.auditName()))
                    .append(SECTION_BREAK).append(failure.header())
                    .append(SECTION_BREAK)
                    .append(format.codeBlock("", descriptions(failure)));
            if (withFixes && placement.inline()) {
                report.append(SECTION_BREAK).append("Suggested fixes (")
                        .append(fixFormat.id()).append("):")
                        .append(SECTION_BREAK).append(fixBlock(List.of(failure),
                                format, fixFormat, fixRenderer, false));
            }
        }

        if (withFixes && placement.section()) {
            report.append(SECTION_BREAK)
                    .append(format.heading(2, "Suggested fixes ("
                            + fixFormat.id() + ")"))
                    .append(SECTION_BREAK).append(fixBlock(ordered, format,
                            fixFormat, fixRenderer, true));
        }

        return report.append(NL).toString();
    }

    private static String summary(final List<DatabaseAuditFailure> failures) {
        final int findingCount =
                failures.stream().mapToInt(f -> f.findings().size()).sum();
        final Set<String> audits = failures.stream()
                .map(f -> f.family() + "/" + f.auditName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return plural(findingCount, "finding") + " across "
                + plural(audits.size(), "audit") + ".";
    }

    private static String descriptions(final DatabaseAuditFailure failure) {
        return failure.findings().stream().map(Finding::description)
                .collect(Collectors.joining(NL));
    }

    private static String fixBlock(final List<DatabaseAuditFailure> failures,
            final ReportFormat format, final FixFormat fixFormat,
            final FixRenderer fixRenderer, final boolean wrapChangelog) {
        if (fixFormat == FixFormat.LIQUIBASE_XML) {
            final List<String> changeSets = failures.stream()
                    .flatMap(failure -> failure.findings().stream()
                            .map(finding -> fixRenderer.liquibaseChangeSet(
                                    finding, failure.platform())))
                    .toList();
            // A standalone changelog must not repeat a change-set id; identical
            // findings render byte-identical change sets, so collapse them.
            final String joined = (wrapChangelog ? changeSets.stream().distinct()
                    : changeSets.stream()).collect(Collectors.joining(NL));
            final String xml = wrapChangelog
                    ? LIQUIBASE_HEADER + NL + joined + NL + "</databaseChangeLog>"
                    : joined;
            return format.codeBlock("xml", xml);
        }
        final String sql = failures.stream()
                .flatMap(failure -> failure.findings().stream().map(finding -> {
                    final Fix fix =
                            fixRenderer.fixFor(finding, failure.platform());
                    return "-- " + failure.auditName() + " ["
                            + fix.fidelity().label() + "]" + NL + fix.ddl();
                })).collect(Collectors.joining(SECTION_BREAK));
        return format.codeBlock("sql", sql);
    }

    private static String plural(final int count, final String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }
}
