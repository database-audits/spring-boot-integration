package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.ForeignKeyIndexAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every foreign key has a supporting index, using
 * {@link ForeignKeyIndexAudit}.
 */
public class ForeignKeyIndexAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Foreign keys with no supporting index — add an index whose leading columns are the FK columns.";

    private final ForeignKeyIndexAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public ForeignKeyIndexAuditAssertion(final ForeignKeyIndexAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every foreign key in the schema has a supporting index.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that every foreign key in the schema has a supporting index,
     * ignoring the excluded constraints.
     *
     * @param schema
     *                                the schema to audit.
     * @param excludedConstraints
     *                                the FK constraint names to skip as
     *                                intentionally unindexed.
     */
    public void assertClean(final String schema,
            final Set<String> excludedConstraints) {
        failOnViolations(MESSAGE, audit.audit(schema, excludedConstraints));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.CATALOG;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.schema(),
                scope.excludes().foreignKeyIndexConstraints());
    }
}
