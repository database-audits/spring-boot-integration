package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.runtime.UnconditionalMutationAudit;

/**
 * Asserts that no captured statement is a full-table UPDATE/DELETE (or is
 * excluded), using {@link UnconditionalMutationAudit}.
 */
public class UnconditionalMutationAuditAssertion
        extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Unconditional UPDATE/DELETE detected (full-table mutation) — add a WHERE clause, or exclude a "
                    + "deliberate full-table statement.";

    private final UnconditionalMutationAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     */
    public UnconditionalMutationAuditAssertion(
            final UnconditionalMutationAudit audit) {
        this.audit = audit;
    }

    /**
     * Asserts that no captured statement is a full-table UPDATE/DELETE.
     */
    public void assertClean() {
        assertClean(Set.of());
    }

    /**
     * Asserts that no captured statement is a full-table UPDATE/DELETE,
     * ignoring the excluded statements.
     *
     * @param excludedStatements
     *                               the exact statements to exclude as
     *                               deliberate full-table mutations.
     */
    public void assertClean(final Set<String> excludedStatements) {
        failOnViolations(MESSAGE, audit.audit(excludedStatements));
    }
}
