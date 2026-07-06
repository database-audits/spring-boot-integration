package io.github.databaseaudits.spring.boot.assertion;

import java.util.Arrays;
import java.util.Set;

import io.github.databaseaudits.audit.jpa.SchemaEntityValidationAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that the JPA entity mappings match the schema, using
 * {@link SchemaEntityValidationAudit}.
 */
public class SchemaEntityValidationAuditAssertion
        extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "JPA entity mappings do not match the schema; reconcile each mapping with the schema "
                    + "(whichever drifted):";

    private final SchemaEntityValidationAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public SchemaEntityValidationAuditAssertion(
            final SchemaEntityValidationAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that the JPA entity mappings match the schema.
     */
    public void assertClean() {
        failOnViolations(MESSAGE, audit.audit());
    }

    /**
     * Asserts that the JPA entity mappings match the schema, ignoring the
     * excluded relations.
     *
     * @param excludedRelations
     *                              relations to skip, each a table name or a
     *                              {@code table.column} pair — optionally
     *                              schema-qualified ({@code schema.table} or
     *                              {@code schema.table.column}) — matched
     *                              case-insensitively.
     */
    public void assertClean(final String... excludedRelations) {
        failOnViolations(MESSAGE,
                audit.audit(Set.copyOf(Arrays.asList(excludedRelations))));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.JPA;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(
                scope.excludes().jpaExcludedRelations().toArray(new String[0]));
    }
}
