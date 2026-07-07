package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;

import io.github.databaseaudits.audit.runtime.RepeatedStatementAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that no captured {@code SELECT} shape ran at least the threshold
 * number of times (or is excluded) — the signature of an N+1 statement burst
 * — using {@link RepeatedStatementAudit}.
 */
public class RepeatedStatementAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Repeated SELECT statement shapes (N+1 burst) — eliminate with a fetch join, @EntityGraph, or "
                    + "@BatchSize/hibernate.default_batch_fetch_size, or exclude the statement if the repetition "
                    + "is deliberate.";

    private final RepeatedStatementAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public RepeatedStatementAuditAssertion(final RepeatedStatementAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that no captured {@code SELECT} shape ran at least
     * {@code threshold} times.
     *
     * @param threshold
     *                      the minimum capture count (inclusive) for a
     *                      statement shape to be reported; must be at least 2.
     */
    public void assertClean(final int threshold) {
        assertClean(threshold, List.of());
    }

    /**
     * Asserts that no captured {@code SELECT} shape ran at least
     * {@code threshold} times, ignoring statements matching the excluded
     * fragments.
     *
     * @param threshold
     *                                 the minimum capture count (inclusive)
     *                                 for a statement shape to be reported;
     *                                 must be at least 2.
     * @param excludedSqlFragments
     *                                 the SQL fragments whose statements to
     *                                 exclude.
     */
    public void assertClean(final int threshold,
            final List<String> excludedSqlFragments) {
        failOnViolations(MESSAGE,
                audit.audit(threshold, excludedSqlFragments));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.RUNTIME;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.excludes().repeatedStatementThreshold(),
                scope.excludes().repeatedStatementSqlFragments());
    }
}
