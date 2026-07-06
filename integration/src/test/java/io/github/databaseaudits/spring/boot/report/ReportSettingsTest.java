package io.github.databaseaudits.spring.boot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * Unit tests for {@link ReportSettings}: absent keys fall back to the documented
 * defaults (including a format-derived output path), configured keys are parsed,
 * and a same-named system property backs a missing configuration parameter.
 */
class ReportSettingsTest {
    @Test
    void testFrom_NoParameters_AppliesDefaults() {
        final ConfigurationParameters parameters =
                mock(ConfigurationParameters.class);

        assertThat(ReportSettings.from(parameters))
                .as("Absent parameters default to an enabled AsciiDoc report with inline + section Liquibase XML fixes.")
                .isEqualTo(new ReportSettings(true, ReportFormat.ASCIIDOC,
                        FixFormat.LIQUIBASE_XML, FixPlacement.BOTH,
                        Path.of("target/database-audit-report.adoc")));
    }

    @Test
    void testFrom_ConfiguredParameters_AreParsed() {
        final ConfigurationParameters parameters =
                mock(ConfigurationParameters.class);
        when(parameters.get(ReportSettings.ENABLED_KEY))
                .thenReturn(Optional.of("false"));
        when(parameters.get(ReportSettings.FORMAT_KEY))
                .thenReturn(Optional.of("asciidoc"));
        when(parameters.get(ReportSettings.FIX_FORMAT_KEY))
                .thenReturn(Optional.of("liquibase-xml"));
        when(parameters.get(ReportSettings.FIX_PLACEMENT_KEY))
                .thenReturn(Optional.of("inline"));
        when(parameters.get(ReportSettings.OUTPUT_FILE_KEY))
                .thenReturn(Optional.of("build/audit.adoc"));

        assertThat(ReportSettings.from(parameters))
                .as("Configured parameters are parsed verbatim.")
                .isEqualTo(new ReportSettings(false, ReportFormat.ASCIIDOC,
                        FixFormat.LIQUIBASE_XML, FixPlacement.INLINE,
                        Path.of("build/audit.adoc")));
    }

    @Test
    void testFrom_FormatWithoutOutputFile_DerivesExtensionFromFormat() {
        final ConfigurationParameters parameters =
                mock(ConfigurationParameters.class);
        when(parameters.get(ReportSettings.FORMAT_KEY))
                .thenReturn(Optional.of("text"));

        assertThat(ReportSettings.from(parameters).outputFile())
                .as("The default output path takes its extension from the format.")
                .isEqualTo(Path.of("target/database-audit-report.txt"));
    }

    @Test
    void testFrom_SystemPropertyFallback_BacksAMissingParameter() {
        final ConfigurationParameters parameters =
                mock(ConfigurationParameters.class);
        System.setProperty(ReportSettings.FORMAT_KEY, "asciidoc");
        try {
            assertThat(ReportSettings.from(parameters).format())
                    .as("A system property backs a configuration parameter the run did not set.")
                    .isEqualTo(ReportFormat.ASCIIDOC);
        } finally {
            System.clearProperty(ReportSettings.FORMAT_KEY);
        }
    }
}
