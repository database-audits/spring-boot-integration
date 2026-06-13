package io.github.databaseaudits.spring.boot.assertion;

import io.github.databaseaudits.audit.jpa.SchemaEntityValidationAudit;

/**
 * Asserts that the JPA entity mappings match the schema, using
 * {@link SchemaEntityValidationAudit}.
 */
public class SchemaEntityValidationAuditAssertion
        extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "JPA entity mappings must match the schema — the EntityManagerFactory must be built under "
                    + "ddl-auto=validate (its startup validation is the real check).";

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
}
