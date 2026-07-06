package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;
import java.util.stream.Collectors;

import io.github.databaseaudits.audit.finding.Finding;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * The {@link AssertionError} an audit assertion throws when its audit finds
 * violations, carrying the structured {@link Finding}s behind the failure so a
 * run-wide listener can render a consolidated report and precise fixes. Being an
 * {@code AssertionError} keeps a finding a test <em>failure</em> rather than an
 * error; the core audits' {@link IllegalStateException} (vacuous capture,
 * unsupported platform) still surfaces cannot-run conditions as errors.
 *
 * <p>
 * The {@linkplain #getMessage() message} is unchanged from the plain assertion
 * this replaces — the curated {@code header}, then each finding's
 * {@link Finding#description() description}, one per line — so existing console
 * output and the {@link DatabaseAuditAssertions} facade's message aggregation are
 * preserved.
 */
public final class DatabaseAuditFailure extends AssertionError {
    private static final long serialVersionUID = 1L;

    private final AuditFamily family;

    private final String auditName;

    private final String header;

    private final List<Finding> findings;

    private final DatabasePlatform platform;

    /**
     * Constructs a failure for one audit's violations.
     *
     * @param family
     *                      the audit family that failed.
     * @param auditName
     *                      the audit's name — its core {@code *Audit} class simple
     *                      name — a stable label the report groups findings by.
     * @param header
     *                      the curated, fix-oriented description of the violation.
     * @param findings
     *                      the structured findings behind the failure; must be
     *                      non-empty.
     * @param platform
     *                      the database platform the audit ran against, so a fix
     *                      renderer can emit dialect-correct DDL.
     */
    public DatabaseAuditFailure(final AuditFamily family, final String auditName,
            final String header, final List<? extends Finding> findings,
            final DatabasePlatform platform) {
        super(header + System.lineSeparator()
                + findings.stream().map(Finding::description)
                        .collect(Collectors.joining(System.lineSeparator())));
        this.family = family;
        this.auditName = auditName;
        this.header = header;
        this.findings = List.copyOf(findings);
        this.platform = platform;
    }

    /**
     * Returns the audit family that failed.
     *
     * @return the audit family.
     */
    public AuditFamily family() {
        return family;
    }

    /**
     * Returns the audit's name — its core {@code *Audit} class simple name.
     *
     * @return the audit name.
     */
    public String auditName() {
        return auditName;
    }

    /**
     * Returns the curated, fix-oriented header describing the violation.
     *
     * @return the failure header.
     */
    public String header() {
        return header;
    }

    /**
     * Returns the structured findings behind this failure.
     *
     * @return the findings.
     */
    public List<Finding> findings() {
        return findings;
    }

    /**
     * Returns the database platform the audit ran against.
     *
     * @return the database platform.
     */
    public DatabasePlatform platform() {
        return platform;
    }
}
