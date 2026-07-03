package io.github.databaseaudits.spring.boot.assertion;

import java.util.HashSet;
import java.util.Set;

import io.github.databaseaudits.audit.catalog.PrimaryKeyPresenceAudit;

/**
 * Asserts that every base table has a primary key (or is excluded), using
 * {@link PrimaryKeyPresenceAudit}.
 */
public class PrimaryKeyPresenceAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Tables with no PRIMARY KEY — add a primary key to each, or exclude it (e.g. Liquibase bookkeeping "
                    + "tables).";

    private final PrimaryKeyPresenceAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     */
    public PrimaryKeyPresenceAuditAssertion(
            final PrimaryKeyPresenceAudit audit) {
        this.audit = audit;
    }

    /**
     * Asserts that every base table has a primary key, excluding the Liquibase
     * bookkeeping tables.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema,
                PrimaryKeyPresenceAudit.LIQUIBASE_BOOKKEEPING_TABLES);
    }

    /**
     * Asserts that every base table has a primary key, ignoring the excluded
     * tables.
     *
     * @param schema
     *                           the schema to audit.
     * @param excludedTables
     *                           the table names to exclude.
     */
    public void assertClean(final String schema,
            final Set<String> excludedTables) {
        failOnViolations(MESSAGE, audit.audit(schema, excludedTables));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.CATALOG;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        final Set<String> excludedTables = new HashSet<>(
                PrimaryKeyPresenceAudit.LIQUIBASE_BOOKKEEPING_TABLES);
        excludedTables.addAll(scope.excludes().primaryKeyTables());
        assertClean(scope.schema(), excludedTables);
    }
}
