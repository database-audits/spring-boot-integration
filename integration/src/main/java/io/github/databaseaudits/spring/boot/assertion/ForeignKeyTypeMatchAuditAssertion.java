package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.ForeignKeyTypeMatchAudit;

/**
 * Asserts that every foreign key column's type matches its referenced column
 * (or is excluded), using {@link ForeignKeyTypeMatchAudit}.
 */
public class ForeignKeyTypeMatchAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Foreign key columns whose declared type differs from the referenced column's — align the types, or "
                    + "exclude the column if the mismatch is deliberate.";

    private final ForeignKeyTypeMatchAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     */
    public ForeignKeyTypeMatchAuditAssertion(
            final ForeignKeyTypeMatchAudit audit) {
        this.audit = audit;
    }

    /**
     * Asserts that every foreign key column's type matches its referenced
     * column.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that every foreign key column's type matches its referenced
     * column, ignoring the excluded columns.
     *
     * @param schema
     *                            the schema to audit.
     * @param excludedColumns
     *                            the {@code table.column} names to exclude as
     *                            deliberate mismatches.
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
                scope.excludes().foreignKeyTypeMatchColumns());
    }
}
