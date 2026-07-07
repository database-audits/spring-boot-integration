package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.jpa.UnmappedDatabaseObjectAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every physical base table and column in the live schema is
 * mapped by a JPA entity (or is excluded), using
 * {@link UnmappedDatabaseObjectAudit}.
 */
public class UnmappedDatabaseObjectAuditAssertion
        extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Unmapped database objects — map the table/column with an entity, drop it via a migration, or "
                    + "exclude it (e.g. migration-tool bookkeeping tables).";

    private final UnmappedDatabaseObjectAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public UnmappedDatabaseObjectAuditAssertion(
            final UnmappedDatabaseObjectAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every physical base table and column in the live schema is
     * mapped.
     */
    public void assertClean() {
        assertClean(Set.of());
    }

    /**
     * Asserts that every physical base table and column in the live schema is
     * mapped, ignoring the excluded relations.
     *
     * @param excludedRelations
     *                              relations to skip, each a table name or a
     *                              {@code table.column} pair — optionally
     *                              schema-qualified — matched
     *                              case-insensitively.
     */
    public void assertClean(final Set<String> excludedRelations) {
        failOnViolations(MESSAGE, audit.audit(excludedRelations));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.JPA;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.excludes().unmappedDatabaseObjectRelations());
    }
}
