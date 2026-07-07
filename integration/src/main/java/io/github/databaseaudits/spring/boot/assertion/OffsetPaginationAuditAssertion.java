package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;

import io.github.databaseaudits.audit.runtime.OffsetPaginationAudit;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Asserts that no captured query paginates with {@code OFFSET} (or is
 * excluded), using {@link OffsetPaginationAudit}.
 */
public class OffsetPaginationAuditAssertion extends AbstractAuditAssertion {
    private static final String MESSAGE =
            "Offset-based pagination — switch to keyset (seek) pagination, or exclude the statement if the "
                    + "pagination is deliberately shallow and bounded.";

    private final OffsetPaginationAudit audit;

    /**
     * Constructs the assertion around its audit.
     *
     * @param audit
     *                  the underlying audit.
     * @param platform
     *                  the database platform the audit runs against, stamped on
     *                  any raised failure for the fix renderer.
     */
    public OffsetPaginationAuditAssertion(final OffsetPaginationAudit audit,
            final DatabasePlatform platform) {
        super(platform);
        this.audit = audit;
    }

    /**
     * Asserts that no captured query paginates with {@code OFFSET}.
     */
    public void assertClean() {
        assertClean(List.of());
    }

    /**
     * Asserts that no captured query paginates with {@code OFFSET}, ignoring
     * statements matching the excluded fragments.
     *
     * @param excludedSqlFragments
     *                                 the SQL fragments whose statements to
     *                                 exclude.
     */
    public void assertClean(final List<String> excludedSqlFragments) {
        failOnViolations(MESSAGE, audit.audit(excludedSqlFragments));
    }

    @Override
    public AuditFamily family() {
        return AuditFamily.RUNTIME;
    }

    @Override
    public void assertClean(final AuditScope scope) {
        assertClean(scope.excludes().offsetPaginationSqlFragments());
    }
}
