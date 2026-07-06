package io.github.databaseaudits.spring.boot.report;

/**
 * A generated remediation for one finding: how faithfully it addresses the
 * finding and the DDL (or, for an {@link FixFidelity#ADVISORY} fix, the guidance
 * comment) that does so.
 *
 * @param fidelity
 *                     how faithfully {@code ddl} remediates the finding.
 * @param ddl
 *                     the SQL that remediates the finding, or an advisory
 *                     comment when no DDL applies; may span several lines.
 */
public record Fix(FixFidelity fidelity, String ddl) {
}
