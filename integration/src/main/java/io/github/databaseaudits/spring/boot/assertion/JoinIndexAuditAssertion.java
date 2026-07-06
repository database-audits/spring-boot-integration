package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;
import java.util.Set;

import io.github.databaseaudits.audit.runtime.plan.JoinIndexAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every captured join key is served by an index (or is excluded),
 * using {@link JoinIndexAudit}.
 */
public class JoinIndexAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Unindexed join key — a Hash/Merge Join or a Nested Loop over an inner Seq Scan survived "
                    + "enable_hashjoin/enable_mergejoin/enable_seqscan=off, so no index can serve the join. Fix: add "
                    + "an index on the joined column(s), or exclude the relation / SQL fragment (FULL OUTER JOINs and "
                    + "small static tables are common deliberate exclusions).";

    private final JoinIndexAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public JoinIndexAuditAssertion(final JoinIndexAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every captured join key is served by an index.
     */
    public void assertClean() {
        assertClean(Set.of(), List.of());
    }

    /**
     * Asserts that every captured join key is served by an index, ignoring the
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
