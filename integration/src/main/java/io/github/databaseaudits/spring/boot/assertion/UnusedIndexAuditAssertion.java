package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.runtime.plan.UnusedIndexAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every index is used by at least one captured statement's plan
 * (or is excluded), using {@link UnusedIndexAudit}.
 */
public class UnusedIndexAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Indexes used by no captured statement's plan — drop the index after confirming against production "
                    + "usage statistics (pg_stat_user_indexes), or exclude it (e.g. an index kept for a rare "
                    + "admin query outside the captured workload).";

    private final UnusedIndexAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public UnusedIndexAuditAssertion(final UnusedIndexAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every index in the schema is used by at least one captured
     * statement's plan.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that every index in the schema is used by at least one captured
     * statement's plan, ignoring the excluded indexes.
     *
     * @param schema
     *                            the schema to audit.
     * @param excludedIndexes
     *                            the index names to exclude.
     */
    public void assertClean(final String schema,
            final Set<String> excludedIndexes) {
        failOnViolations(MESSAGE, audit.audit(schema, excludedIndexes));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.RUNTIME;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.schema(), scope.excludes().unusedIndexes());
    }
}
