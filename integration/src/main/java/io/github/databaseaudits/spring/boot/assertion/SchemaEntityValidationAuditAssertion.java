package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.jpa.SchemaEntityValidationAudit;

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
     */
    public SchemaEntityValidationAuditAssertion(
            final SchemaEntityValidationAudit audit) {
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
        failOnViolations(MESSAGE, audit.audit(Set.of(excludedRelations)));
    }
}
