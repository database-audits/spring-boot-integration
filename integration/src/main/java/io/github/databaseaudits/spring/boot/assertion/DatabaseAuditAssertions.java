package io.github.databaseaudits.spring.boot.assertion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.databaseaudits.audit.catalog.PrimaryKeyPresenceAudit;

/**
 * Convenience facade that runs whole audit families in one call. Each family
 * method runs every audit in the family and, if any find violations, throws a
 * single {@link AssertionError} aggregating all of their messages (rather than
 * failing fast on the first). Genuine cannot-run conditions — the core audits'
 * {@link IllegalStateException} (vacuous capture, unsupported platform) —
 * propagate immediately. For per-audit granularity, inject the individual
 * {@code *AuditAssertion} beans instead.
 */
public class DatabaseAuditAssertions {
    private final ForeignKeyIndexAuditAssertion foreignKeyIndex;
    private final ForeignKeyNotNullAuditAssertion foreignKeyNotNull;
    private final ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatch;
    private final PrimaryKeyPresenceAuditAssertion primaryKeyPresence;
    private final RedundantIndexAuditAssertion redundantIndex;
    private final SchemaEntityValidationAuditAssertion schemaEntityValidation;
    private final JoinIndexAuditAssertion joinIndex;
    private final OrderByIndexAuditAssertion orderByIndex;
    private final WhereClauseIndexAuditAssertion whereClauseIndex;
    private final UnconditionalMutationAuditAssertion unconditionalMutation;

    /**
     * Constructs the facade from the per-audit assertions.
     *
     * @param foreignKeyIndex
     *                                   the foreign-key index assertion.
     * @param foreignKeyNotNull
     *                                   the foreign-key not-null assertion.
     * @param foreignKeyTypeMatch
     *                                   the foreign-key type-match assertion.
     * @param primaryKeyPresence
     *                                   the primary-key presence assertion.
     * @param redundantIndex
     *                                   the redundant-index assertion.
     * @param schemaEntityValidation
     *                                   the JPA schema/entity validation
     *                                   assertion.
     * @param joinIndex
     *                                   the join-index assertion.
     * @param orderByIndex
     *                                   the order-by index assertion.
     * @param whereClauseIndex
     *                                   the where-clause index assertion.
     * @param unconditionalMutation
     *                                   the unconditional-mutation assertion.
     */
    public DatabaseAuditAssertions(
            final ForeignKeyIndexAuditAssertion foreignKeyIndex,
            final ForeignKeyNotNullAuditAssertion foreignKeyNotNull,
            final ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatch,
            final PrimaryKeyPresenceAuditAssertion primaryKeyPresence,
            final RedundantIndexAuditAssertion redundantIndex,
            final SchemaEntityValidationAuditAssertion schemaEntityValidation,
            final JoinIndexAuditAssertion joinIndex,
            final OrderByIndexAuditAssertion orderByIndex,
            final WhereClauseIndexAuditAssertion whereClauseIndex,
            final UnconditionalMutationAuditAssertion unconditionalMutation) {
        this.foreignKeyIndex = foreignKeyIndex;
        this.foreignKeyNotNull = foreignKeyNotNull;
        this.foreignKeyTypeMatch = foreignKeyTypeMatch;
        this.primaryKeyPresence = primaryKeyPresence;
        this.redundantIndex = redundantIndex;
        this.schemaEntityValidation = schemaEntityValidation;
        this.joinIndex = joinIndex;
        this.orderByIndex = orderByIndex;
        this.whereClauseIndex = whereClauseIndex;
        this.unconditionalMutation = unconditionalMutation;
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
        assertAll(
                () -> foreignKeyIndex.assertClean(schema,
                        excludes.foreignKeyIndexConstraints()),
                () -> foreignKeyNotNull.assertClean(schema,
                        excludes.foreignKeyNotNullColumns()),
                () -> foreignKeyTypeMatch.assertClean(schema,
                        excludes.foreignKeyTypeMatchColumns()),
                () -> primaryKeyPresence.assertClean(schema,
                        primaryKeyExcludes(excludes)),
                () -> redundantIndex.assertClean(schema,
                        excludes.redundantIndexes()));
    }

    /**
     * Asserts that the JPA mapping audit is clean.
     */
    public void assertJpaClean() {
        schemaEntityValidation.assertClean();
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
        assertAll(
                () -> joinIndex.assertClean(excludes.planRelations(),
                        excludes.planSqlFragments()),
                () -> orderByIndex.assertClean(excludes.planRelations(),
                        excludes.planSqlFragments()),
                () -> whereClauseIndex.assertClean(excludes.planRelations(),
                        excludes.planSqlFragments()),
                () -> unconditionalMutation.assertClean(
                        excludes.unconditionalMutationStatements()));
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
        assertAll(() -> assertCatalogClean(schema, excludes),
                this::assertJpaClean, () -> assertRuntimeClean(excludes));
    }

    private static Set<String> primaryKeyExcludes(
            final DatabaseAuditExcludes excludes) {
        final Set<String> tables = new HashSet<>(
                PrimaryKeyPresenceAudit.LIQUIBASE_BOOKKEEPING_TABLES);
        tables.addAll(excludes.primaryKeyTables());
        return tables;
    }

    private static void assertAll(final Runnable... checks) {
        final List<String> failures = new ArrayList<>();
        for (final Runnable check : checks) {
            try {
                check.run();
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
