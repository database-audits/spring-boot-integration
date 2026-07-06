package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;
import java.util.Set;

import io.github.databaseaudits.audit.runtime.plan.WhereClauseIndexAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every captured WHERE-clause access is index-satisfiable (or is
 * excluded), using {@link WhereClauseIndexAudit}.
 */
public class WhereClauseIndexAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Unindexed WHERE-clause access — a Seq Scan with a Filter survived enable_seqscan=off, so no index can "
                    + "satisfy the predicate. Fix: add an index on the filtered column(s), or exclude the relation / "
                    + "SQL fragment.";

    private final WhereClauseIndexAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public WhereClauseIndexAuditAssertion(final WhereClauseIndexAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every captured WHERE-clause access is index-satisfiable.
     */
    public void assertClean() {
        assertClean(Set.of(), List.of());
    }

    /**
     * Asserts that every captured WHERE-clause access is index-satisfiable,
     * ignoring the excluded relations and fragments.
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
