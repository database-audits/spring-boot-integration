package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.PrimaryKeyTypeAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every primary key is at least {@code bigint} wide (or excluded),
 * using {@link PrimaryKeyTypeAudit}.
 */
public class PrimaryKeyTypeAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Primary keys narrower than bigint — migrate the column to BIGINT (widening any referencing foreign "
                    + "key columns in the same change), or exclude it if the table is genuinely bounded.";

    private final PrimaryKeyTypeAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public PrimaryKeyTypeAuditAssertion(final PrimaryKeyTypeAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every primary key in the schema is at least {@code bigint}
     * wide.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that every primary key in the schema is at least {@code bigint}
     * wide, ignoring the excluded columns.
     *
     * @param schema
     *                            the schema to audit.
     * @param excludedColumns
     *                            the {@code table.column} names to exclude as
     *                            genuinely bounded.
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
        assertClean(scope.schema(), scope.excludes().primaryKeyTypeColumns());
    }
}
