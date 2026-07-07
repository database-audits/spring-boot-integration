package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.UniqueIndexNotNullAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that no UNIQUE index includes a nullable column (or is excluded),
 * using {@link UniqueIndexNotNullAudit}.
 */
public class UniqueIndexNotNullAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "UNIQUE indexes over nullable columns — rows with NULLs bypass uniqueness. Make the column NOT NULL, "
                    + "use PostgreSQL 15+ NULLS NOT DISTINCT, or exclude the index if the partial uniqueness is "
                    + "deliberate.";

    private final UniqueIndexNotNullAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public UniqueIndexNotNullAuditAssertion(
            final UniqueIndexNotNullAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that no UNIQUE index in the schema includes a nullable column.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that no UNIQUE index in the schema includes a nullable column,
     * ignoring the excluded indexes.
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
        return AuditFamily.CATALOG;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.schema(), scope.excludes().uniqueIndexNotNullIndexes());
    }
}
