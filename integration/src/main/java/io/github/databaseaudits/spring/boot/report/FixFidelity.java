package io.github.databaseaudits.spring.boot.report;

/**
 * How faithfully a generated fix addresses its finding. The report labels every
 * non-{@link #PRECISE} fix so a reader reviews it before applying — and in
 * Liquibase output only {@code PRECISE} fixes become runnable change sets; the
 * rest are emitted as comments so the changelog still applies cleanly.
 */
public enum FixFidelity {
    /** Executable DDL that fully remediates the finding. */
    PRECISE("precise"),

    /** DDL with a {@code TODO} the reader must complete before applying. */
    TEMPLATE("template — complete the TODO before applying"),

    /**
     * A suggestion derived from the query plan; the columns are inferred and
     * must be verified.
     */
    BEST_EFFORT("best-effort — verify the columns against the plan"),

    /** No DDL — the fix is a code or query change, shown only as guidance. */
    ADVISORY("advisory — change the code/query, no DDL");

    private final String label;

    FixFidelity(final String label) {
        this.label = label;
    }

    /**
     * Returns the human-readable label the report annotates a fix with.
     *
     * @return the fidelity label.
     */
    public String label() {
        return label;
    }
}
