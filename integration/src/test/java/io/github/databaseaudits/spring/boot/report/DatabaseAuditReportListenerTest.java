package io.github.databaseaudits.spring.boot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import io.github.databaseaudits.audit.finding.ForeignKeyIndexFinding;
import io.github.databaseaudits.platform.DatabasePlatform;
import io.github.databaseaudits.spring.boot.assertion.AuditFamily;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditFailure;

/**
 * Drives {@link DatabaseAuditReportListener} through a real JUnit Platform
 * {@link Launcher} running synthetic scenarios: a run that raises a
 * {@link DatabaseAuditFailure} produces a report, while a clean or disabled run
 * writes nothing — the inertness that keeps unrelated consumer test runs
 * untouched.
 */
class DatabaseAuditReportListenerTest {
    @Test
    void testRun_WithAuditFailure_WritesTheConsolidatedReport(
            @TempDir final Path tempDir) throws IOException {
        final Path report = tempDir.resolve("database-audit-report.md");

        runScenario(AuditFailingScenario.class,
                ReportSettings.OUTPUT_FILE_KEY, report.toString(),
                ReportSettings.FORMAT_KEY, "markdown");

        assertThat(report).as("A run with findings writes the report file.")
                .exists();
        assertThat(Files.readString(report))
                .as("The report titles the run and lists the finding.")
                .contains("# Database Audit Report").contains(
                        "child.fk_child_parent  ->  FOREIGN KEY (parent_id) REFERENCES parent");
    }

    @Test
    void testRun_WithSqlFixFormat_WritesTheFixIntoTheReport(
            @TempDir final Path tempDir) throws IOException {
        final Path report = tempDir.resolve("database-audit-report.md");

        runScenario(AuditFailingScenario.class,
                ReportSettings.OUTPUT_FILE_KEY, report.toString(),
                ReportSettings.FIX_FORMAT_KEY, "sql");

        assertThat(Files.readString(report))
                .as("The configured SQL fix format reaches the written report.")
                .contains(
                        "CREATE INDEX ix_child_parent_id ON child (parent_id);");
    }

    @Test
    void testRun_Clean_WritesNoReport(@TempDir final Path tempDir) {
        final Path report = tempDir.resolve("database-audit-report.md");

        runScenario(PassingScenario.class, ReportSettings.OUTPUT_FILE_KEY,
                report.toString());

        assertThat(report)
                .as("A clean run is inert — it writes no report file.")
                .doesNotExist();
    }

    @Test
    void testRun_DisabledWithFindings_WritesNoReport(
            @TempDir final Path tempDir) {
        final Path report = tempDir.resolve("database-audit-report.md");

        runScenario(AuditFailingScenario.class, ReportSettings.OUTPUT_FILE_KEY,
                report.toString(), ReportSettings.ENABLED_KEY, "false");

        assertThat(report)
                .as("Disabling the report suppresses it even when there are findings.")
                .doesNotExist();
    }

    private static void runScenario(final Class<?> scenario,
            final String... configurationPairs) {
        final LauncherDiscoveryRequestBuilder builder =
                request().selectors(selectClass(scenario))
                        .enableImplicitConfigurationParameters(false);
        for (int i = 0; i < configurationPairs.length; i += 2) {
            builder.configurationParameter(configurationPairs[i],
                    configurationPairs[i + 1]);
        }
        final Launcher launcher = LauncherFactory.create(LauncherConfig.builder()
                .enableTestExecutionListenerAutoRegistration(false).build());
        launcher.execute(builder.build(), new DatabaseAuditReportListener());
    }

    /** A synthetic test that raises a database-audit failure. */
    static class AuditFailingScenario {
        @Test
        void raisesAuditFailure() {
            throw new DatabaseAuditFailure(AuditFamily.CATALOG,
                    "ForeignKeyIndexAudit",
                    "Foreign keys with no supporting index.",
                    List.of(new ForeignKeyIndexFinding("child",
                            "fk_child_parent", List.of("parent_id"), "parent")),
                    DatabasePlatform.POSTGRESQL);
        }
    }

    /** A synthetic test that passes, leaving the run clean. */
    static class PassingScenario {
        @Test
        void passes() {
        }
    }
}
