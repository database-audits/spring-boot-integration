package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;
import java.util.Set;

import io.github.databaseaudits.audit.runtime.plan.OrderByIndexAudit;

/**
 * Asserts that every captured ORDER BY is served by an index, not an explicit
 * sort (or is excluded), using {@link OrderByIndexAudit}.
 */
public class OrderByIndexAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Unindexed ORDER BY — an explicit Sort survived enable_sort=off, so no index (or only a leading prefix) "
                    + "can provide this ordering. Fix: add an index matching the ORDER BY columns (including ASC/DESC "
                    + "and NULLS order), or exclude the relation / SQL fragment.";

    private final OrderByIndexAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     */
    public OrderByIndexAuditAssertion(final OrderByIndexAudit audit) {
        this.audit = audit;
    }

    /**
     * Asserts that every captured ORDER BY is served by an index.
     */
    public void assertClean() {
        assertClean(Set.of(), List.of());
    }

    /**
     * Asserts that every captured ORDER BY is served by an index, ignoring the
     * excluded relations and fragments.
     *
     * @param excludedRelations
     *                                 the relation names to exclude.
     * @param excludedSqlFragments
     *                                 the SQL fragments whose statements to
     *                                 exclude.
     */
    public void assertClean(final Set<String> excludedRelations,
            final List<String> excludedSqlFragments) {
        failOnViolations(MESSAGE,
                audit.audit(excludedRelations, excludedSqlFragments));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.RUNTIME;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.excludes().planRelations(),
                scope.excludes().planSqlFragments());
    }
}
