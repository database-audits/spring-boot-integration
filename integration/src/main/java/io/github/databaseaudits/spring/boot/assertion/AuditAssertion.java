package io.github.databaseaudits.spring.boot.assertion;

/**
 * Uniform entry point shared by every {@code *AuditAssertion}, so the
 * {@link DatabaseAuditAssertions} facade and the Spring wiring can drive the
 * whole roster generically instead of enumerating each audit. Each assertion
 * also keeps its own typed {@code assertClean(...)} overload(s) for direct
 * injection; this interface is the family-agnostic driver those overloads back.
 */
public interface AuditAssertion {
    /**
     * Returns the family this assertion belongs to.
     *
     * @return the audit family.
     */
    AuditFamily family();

    /**
     * Runs the audit for the given scope, throwing an {@link AssertionError}
     * describing the violations if any are found.
     *
     * @param scope
     *                  the schema and exclusions to apply.
     */
    void assertClean(AuditScope scope);
}
