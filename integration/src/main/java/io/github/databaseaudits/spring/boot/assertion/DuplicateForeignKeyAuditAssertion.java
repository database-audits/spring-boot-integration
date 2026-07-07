package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.catalog.DuplicateForeignKeyAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that no relationship is enforced by more than one foreign key
 * constraint (or the duplicates are excluded), using
 * {@link DuplicateForeignKeyAudit}.
 */
public class DuplicateForeignKeyAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Duplicate foreign key constraints — drop all but one of the duplicates, or exclude one to keep the "
                    + "duplication deliberately.";

    private final DuplicateForeignKeyAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public DuplicateForeignKeyAuditAssertion(
            final DuplicateForeignKeyAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that no relationship in the schema is enforced by more than one
     * foreign key constraint.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertClean(final String schema) {
        assertClean(schema, Set.of());
    }

    /**
     * Asserts that no relationship in the schema is enforced by more than one
     * foreign key constraint, ignoring the excluded constraints.
     *
     * @param schema
     *                                The schema to audit.
     * @param excludedConstraints
     *                                the constraint names to exclude.
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
                scope.excludes().duplicateForeignKeyConstraints());
    }
}
