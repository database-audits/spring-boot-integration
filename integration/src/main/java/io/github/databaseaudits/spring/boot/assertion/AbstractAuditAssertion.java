package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;

import io.github.databaseaudits.audit.finding.Finding;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Base for the audit assertions. On violations it throws a
 * {@link DatabaseAuditFailure} — an {@link AssertionError} carrying the curated,
 * fix-oriented message plus the audit's structured {@link Finding}s and the
 * {@link DatabasePlatform} it ran against — so a violation registers as a test
 * <em>failure</em> a run-wide report listener can pick up. Genuine cannot-run
 * conditions (vacuous capture, unsupported platform) keep surfacing as the core
 * audits' {@link IllegalStateException}.
 */
abstract class AbstractAuditAssertion implements AuditAssertion {
    private final DatabasePlatform platform;

    /**
     * Records the platform the suite resolved for this assertion's datasource,
     * so a raised {@link DatabaseAuditFailure} can stamp it for the fix renderer.
     *
     * @param platform
     *                     the database platform this assertion's audit runs
     *                     against.
     */
    protected AbstractAuditAssertion(final DatabasePlatform platform) {
        this.platform = platform;
    }

    /**
     * Throws a {@link DatabaseAuditFailure} if the audit returned any findings.
     *
     * @param header
     *                     the curated description of the violation and its fix.
     * @param findings
     *                     the audit findings; an empty list means clean.
     */
    protected final void failOnViolations(final String header,
            final List<? extends Finding> findings) {
        if (!findings.isEmpty()) {
            throw new DatabaseAuditFailure(family(), auditName(), header,
                    findings, platform);
        }
    }

    /**
     * The audit's name — this assertion's simple name with the {@code Assertion}
     * suffix removed (e.g. {@code ForeignKeyIndexAudit}) — a stable label the
     * report groups findings by.
     */
    private String auditName() {
        final String name = getClass().getSimpleName();
        return name.endsWith("Assertion")
                ? name.substring(0, name.length() - "Assertion".length())
                : name;
    }
}
