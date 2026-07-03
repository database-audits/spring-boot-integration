package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;

/**
 * Base for the audit assertions. Throws an {@link AssertionError} carrying the
 * curated, fix-oriented message and the audit's findings, so a violation
 * registers as a test <em>failure</em> rather than an error. Genuine cannot-run
 * conditions (vacuous capture, unsupported platform) keep surfacing as the core
 * audits' {@link IllegalStateException}.
 */
abstract class AbstractAuditAssertion implements AuditAssertion {
    /**
     * Throws an {@link AssertionError} if the audit returned any violations.
     *
     * @param message
     *                       the curated description of the violation and its
     *                       fix.
     * @param violations
     *                       the audit findings; an empty list means clean.
     */
    protected final void failOnViolations(final String message,
            final List<String> violations) {
        if (!violations.isEmpty()) {
            final String detail =
                    String.join(System.lineSeparator(), violations);
            throw new AssertionError(message + System.lineSeparator() + detail);
        }
    }
}
