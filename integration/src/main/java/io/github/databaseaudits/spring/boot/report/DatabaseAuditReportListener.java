package io.github.databaseaudits.spring.boot.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditFailure;

/**
 * A JUnit Platform {@link TestExecutionListener} that collects every
 * {@link DatabaseAuditFailure} a run produces and, when the run finishes, writes
 * a single consolidated report — so findings scattered across per-audit test
 * failures land in one file instead of the build console.
 *
 * <p>
 * It is auto-registered through
 * {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener},
 * so it loads in every test run the integration jar is on the classpath for.
 * That makes staying <em>inert unless there are findings</em> essential: when no
 * {@link DatabaseAuditFailure} is collected it writes nothing, leaving unrelated
 * consumer test runs untouched. The collector is thread-safe, so audits may run
 * in parallel.
 *
 * <p>
 * Output is controlled by the {@link ReportSettings} configuration parameters,
 * read from the run's {@code junit-platform.properties} (with system-property
 * fallback).
 */
public class DatabaseAuditReportListener implements TestExecutionListener {
    private static final System.Logger LOGGER =
            System.getLogger(DatabaseAuditReportListener.class.getName());

    private final List<DatabaseAuditFailure> failures =
            new CopyOnWriteArrayList<>();

    /**
     * Clears any failures collected from a previous run, so this listener —
     * loaded once via {@code ServiceLoader} and reused across runs in the same
     * JVM — starts each run with a clean slate.
     *
     * @param testPlan
     *                     the plan about to execute; unused.
     */
    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        failures.clear();
    }

    /**
     * Collects the test's failure if it is a {@link DatabaseAuditFailure}; any
     * other outcome — success, a plain failure, or a skip — is ignored.
     *
     * @param testIdentifier
     *                                the finished test; unused.
     * @param testExecutionResult
     *                                the test's outcome.
     */
    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
            final TestExecutionResult testExecutionResult) {
        testExecutionResult.getThrowable()
                .filter(DatabaseAuditFailure.class::isInstance)
                .map(DatabaseAuditFailure.class::cast).ifPresent(failures::add);
    }

    /**
     * Writes the consolidated report once the run finishes, unless no
     * {@link DatabaseAuditFailure} was collected or the report is disabled.
     *
     * @param testPlan
     *                     the finished plan, whose configuration parameters
     *                     resolve the {@link ReportSettings}.
     */
    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        if (failures.isEmpty()) {
            return;
        }
        final ReportSettings settings =
                ReportSettings.from(testPlan.getConfigurationParameters());
        if (!settings.enabled()) {
            return;
        }
        writeReport(settings, List.copyOf(failures));
    }

    private void writeReport(final ReportSettings settings,
            final List<DatabaseAuditFailure> collected) {
        final String report = DatabaseAuditReport.render(collected,
                settings.format(), settings.fixFormat(), settings.placement(),
                new FixRenderer());
        final Path outputFile = settings.outputFile();
        try {
            final Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputFile, report);
            LOGGER.log(System.Logger.Level.INFO,
                    () -> "Wrote database audit report to "
                            + outputFile.toAbsolutePath());
        } catch (final IOException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Failed to write database audit report to "
                            + outputFile.toAbsolutePath(),
                    e);
        }
    }
}
