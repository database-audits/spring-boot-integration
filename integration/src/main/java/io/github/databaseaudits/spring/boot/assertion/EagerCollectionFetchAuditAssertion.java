package io.github.databaseaudits.spring.boot.assertion;

import java.util.Set;

import io.github.databaseaudits.audit.jpa.EagerCollectionFetchAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that no mapped collection is fetched eagerly (or is excluded), using
 * {@link EagerCollectionFetchAudit}.
 */
public class EagerCollectionFetchAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Eagerly fetched collections — switch to FetchType.LAZY and fetch eagerly only where a specific "
                    + "query genuinely needs it (fetch join or @EntityGraph), or exclude the role.";

    private final EagerCollectionFetchAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public EagerCollectionFetchAuditAssertion(
            final EagerCollectionFetchAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that no mapped collection is fetched eagerly.
     */
    public void assertClean() {
        assertClean(Set.of());
    }

    /**
     * Asserts that no mapped collection is fetched eagerly, ignoring the
     * excluded roles.
     *
     * @param excludedRoles
     *                          the collection roles to exclude (e.g.
     *                          {@code com.acme.Order.items}).
     */
    public void assertClean(final Set<String> excludedRoles) {
        failOnViolations(MESSAGE, audit.audit(excludedRoles));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.JPA;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.excludes().eagerCollectionRoles());
    }
}
