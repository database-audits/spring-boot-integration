package io.github.databaseaudits.spring.boot.assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Convenience facade that runs whole audit families in one call. Each family
 * method runs every audit in the family and, if any find violations, throws a
 * single {@link AssertionError} aggregating all of their messages (rather than
 * failing fast on the first). Genuine cannot-run conditions — the core audits'
 * {@link IllegalStateException} (vacuous capture, unsupported platform) —
 * propagate immediately. For per-audit granularity, inject the individual
 * {@code *AuditAssertion} beans instead.
 *
 * <p>
 * The facade drives the audits through the family-agnostic
 * {@link AuditAssertion} contract, so it iterates the suite's roster rather than
 * enumerating each audit — adding an audit needs no change here.
 */
public class DatabaseAuditAssertions {
    private final List<AuditAssertion> assertions;

    /**
     * Constructs the facade from the suite's audit assertions.
     *
     * @param assertions
     *                       every audit assertion to run, in the order the suite
     *                       wires them.
     */
    public DatabaseAuditAssertions(final List<AuditAssertion> assertions) {
        this.assertions = List.copyOf(assertions);
    }

    /**
     * Asserts that the catalog audits are clean for the schema, with no
     * exclusions.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertCatalogClean(final String schema) {
        assertCatalogClean(schema, DatabaseAuditExcludes.none());
    }

    /**
     * Asserts that the catalog audits are clean for the schema, ignoring the
     * given exclusions (the Liquibase bookkeeping tables are always excluded
     * from the primary-key audit).
     *
     * @param schema
     *                     the schema to audit.
     * @param excludes
     *                     the exclusions to apply.
     */
    public void assertCatalogClean(final String schema,
            final DatabaseAuditExcludes excludes) {
        assertFamily(AuditFamily.CATALOG, new AuditScope(schema, excludes));
    }

    /**
     * Asserts that the JPA mapping audit is clean.
     */
    public void assertJpaClean() {
        assertFamily(AuditFamily.JPA,
                new AuditScope(null, DatabaseAuditExcludes.none()));
    }

    /**
     * Asserts that the runtime SQL audits are clean for the SQL captured so
     * far, with no exclusions.
     */
    public void assertRuntimeClean() {
        assertRuntimeClean(DatabaseAuditExcludes.none());
    }

    /**
     * Asserts that the runtime SQL audits are clean for the SQL captured so
     * far, ignoring the given exclusions.
     *
     * @param excludes
     *                     the exclusions to apply.
     */
    public void assertRuntimeClean(final DatabaseAuditExcludes excludes) {
        assertFamily(AuditFamily.RUNTIME, new AuditScope(null, excludes));
    }

    /**
     * Asserts that every audit family is clean, with no exclusions.
     *
     * @param schema
     *                   the schema to audit.
     */
    public void assertAllClean(final String schema) {
        assertAllClean(schema, DatabaseAuditExcludes.none());
    }

    /**
     * Asserts that every audit family is clean, ignoring the given exclusions;
     * failures across all families are aggregated into one error.
     *
     * @param schema
     *                     the schema to audit.
     * @param excludes
     *                     the exclusions to apply.
     */
    public void assertAllClean(final String schema,
            final DatabaseAuditExcludes excludes) {
        assertMatching(assertion -> true, new AuditScope(schema, excludes));
    }

    private void assertFamily(final AuditFamily family,
            final AuditScope scope) {
        assertMatching(assertion -> assertion.family() == family, scope);
    }

    private void assertMatching(final Predicate<AuditAssertion> selector,
            final AuditScope scope) {
        final List<String> failures = new ArrayList<>();
        for (final AuditAssertion assertion : assertions) {
            if (!selector.test(assertion)) {
                continue;
            }
            try {
                assertion.assertClean(scope);
            } catch (final AssertionError failure) {
                failures.add(failure.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            final String separator =
                    System.lineSeparator() + System.lineSeparator();
            throw new AssertionError(String.join(separator, failures));
        }
    }
}
