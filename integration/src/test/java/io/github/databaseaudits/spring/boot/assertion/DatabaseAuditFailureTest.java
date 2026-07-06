package io.github.databaseaudits.spring.boot.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.databaseaudits.audit.catalog.ForeignKeyIndexAudit;
import io.github.databaseaudits.audit.finding.Finding;
import io.github.databaseaudits.audit.finding.ForeignKeyIndexFinding;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Tests that an assertion raises a structured {@link DatabaseAuditFailure} on
 * violations — carrying the family, audit name, header, findings, and platform —
 * while preserving the historical message text, and stays silent when clean.
 * Exercised through {@link ForeignKeyIndexAuditAssertion} as a representative of
 * the shared {@link AbstractAuditAssertion} behavior.
 */
class DatabaseAuditFailureTest {
    private final ForeignKeyIndexAudit audit = mock(ForeignKeyIndexAudit.class);

    @Test
    void testAssertClean_WithViolations_ThrowsStructuredFailure() {
        final Finding finding = new ForeignKeyIndexFinding("child",
                "fk_child_parent", List.of("parent_id"), "parent");
        when(audit.audit("public", Set.of())).thenReturn(List.of(finding));
        final ForeignKeyIndexAuditAssertion assertion =
                new ForeignKeyIndexAuditAssertion(audit,
                        DatabasePlatform.POSTGRESQL);

        final Throwable thrown = catchThrowable(() -> assertion.assertClean("public"));

        assertThat(thrown).as("A violation is a DatabaseAuditFailure.")
                .isInstanceOf(DatabaseAuditFailure.class);
        final DatabaseAuditFailure failure = (DatabaseAuditFailure) thrown;
        assertThat(failure.family()).as("The audit family is carried.")
                .isEqualTo(AuditFamily.CATALOG);
        assertThat(failure.auditName())
                .as("The audit name is the assertion's name without the Assertion suffix.")
                .isEqualTo("ForeignKeyIndexAudit");
        assertThat(failure.findings()).as("The structured findings are carried.")
                .containsExactly(finding);
        assertThat(failure.platform()).as("The resolved platform is carried.")
                .isEqualTo(DatabasePlatform.POSTGRESQL);
        assertThat(failure.getMessage())
                .as("The message is the header then each finding's description, unchanged.")
                .isEqualTo(failure.header() + System.lineSeparator()
                        + finding.description());
    }

    @Test
    void testAssertClean_NoViolations_DoesNotThrow() {
        when(audit.audit("public", Set.of())).thenReturn(List.of());
        final ForeignKeyIndexAuditAssertion assertion =
                new ForeignKeyIndexAuditAssertion(audit,
                        DatabasePlatform.POSTGRESQL);

        assertThatCode(() -> assertion.assertClean("public"))
                .as("A clean audit raises nothing.").doesNotThrowAnyException();
    }
}
