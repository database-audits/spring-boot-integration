package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.RedundantIndexAudit;

/**
 * Asserts that the schema has no redundant indexes (or only excluded ones),
 * using {@link RedundantIndexAudit}.
 */
public class RedundantIndexAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Redundant indexes — drop the narrower index (its lookups are served by the wider one), or exclude it.";

    private final RedundantIndexAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     */
    public RedundantIndexAuditAssertion(final RedundantIndexAudit audit) {
        this.audit = audit;
    }

    /**
     * Asserts that the schema has no redundant indexes.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that the schema has no redundant indexes, ignoring the excluded
     * indexes.
     *
     * @param schema
     *                            the schema to audit.
     * @param excludedIndexes
     *                            the index names to exclude as intentional
     *                            look-alikes.
     */
    public void assertClean(final String schema,
            final Set<String> excludedIndexes) {
        failOnViolations(MESSAGE, audit.audit(schema, excludedIndexes));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.CATALOG;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.schema(), scope.excludes().redundantIndexes());
    }
}
