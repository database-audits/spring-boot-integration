package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.ForeignKeyNotNullAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every foreign key column is NOT NULL (or excluded), using
 * {@link ForeignKeyNotNullAudit}.
 */
public class ForeignKeyNotNullAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Nullable foreign key columns — make each column NOT NULL, or exclude it if the relationship is "
                    + "genuinely optional.";

    private final ForeignKeyNotNullAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public ForeignKeyNotNullAuditAssertion(final ForeignKeyNotNullAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every foreign key column in the schema is NOT NULL.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that every foreign key column in the schema is NOT NULL, ignoring
     * the excluded columns.
     *
     * @param schema
     *                            the schema to audit.
     * @param excludedColumns
     *                            the {@code table.column} names to exclude as
     *                            genuinely optional.
     */
    public void assertClean(final String schema,
            final Set<String> excludedColumns) {
        failOnViolations(MESSAGE, audit.audit(schema, excludedColumns));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.CATALOG;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.schema(),
                scope.excludes().foreignKeyNotNullColumns());
    }
}
