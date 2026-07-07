package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.jpa.MissingVersionAttributeAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that every mutable root entity carries a {@code @Version} attribute
 * (or is excluded), using {@link MissingVersionAttributeAudit}.
 */
public class MissingVersionAttributeAuditAssertion
        extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Mutable entities with no @Version attribute — concurrent updates can silently overwrite each "
                    + "other's changes. Add a @Version attribute, or exclude the entity if it is genuinely "
                    + "append-only or single-writer.";

    private final MissingVersionAttributeAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public MissingVersionAttributeAuditAssertion(
            final MissingVersionAttributeAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that every mutable root entity carries a {@code @Version}
     * attribute.
     */
    public void assertClean() {
        assertClean(Set.of());
    }

    /**
     * Asserts that every mutable root entity carries a {@code @Version}
     * attribute, ignoring the excluded entities.
     *
     * @param excludedEntities
     *                             the entities to exclude — fully-qualified
     *                             name, simple name, or physical table name.
     */
    public void assertClean(final Set<String> excludedEntities) {
        failOnViolations(MESSAGE, audit.audit(excludedEntities));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.JPA;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.excludes().missingVersionEntities());
    }
}
