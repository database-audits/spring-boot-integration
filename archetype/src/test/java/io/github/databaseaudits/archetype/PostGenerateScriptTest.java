package io.github.databaseaudits.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * Verifies the archetype post-generate script behavior: the {@code tests-only} generation mode —
 * particularly that {@code projectDirectory} is honored whether it arrives in {@code request.properties}
 * (the archetype integration-test mojo) or only as a JVM system property (the {@code archetype:generate}
 * command line, where {@code projectDirectory} is not a declared archetype property) — and that the
 * {@code multi/} token template expands into a {@code DatabaseAudit<Name>TestConfiguration} per
 * {@code dataSourceNames} entry (plus a {@code DatabaseAuditMultiTestConfiguration} aggregator), or is removed
 * when no names are given.
 */
class PostGenerateScriptTest {

    private static final String PACKAGE = "com.example.demo";
    private static final String PACKAGE_PATH = "com/example/demo";

    @TempDir
    Path tempDir;

    @Test
    void testPostGenerate_TestsOnlyWithAbsoluteProjectDirectory_CopiesAuditFilesToProjectDir()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);

        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScript(outputDir, "demo", "tests-only", "none",
                projectDir.toAbsolutePath().toString(), null);

        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("Audit IT file should be copied to the project directory.")
                .exists();
        assertThat(projectDir.resolve("src/test/resources/junit-platform.properties"))
                .as("junit-platform.properties should be copied to the project directory.")
                .exists();
        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/DemoApplication.java"))
                .as("DemoApplication.java should not be in the project directory.")
                .doesNotExist();
        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/app"))
                .as("app/ directory should not be in the project directory.")
                .doesNotExist();
        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/runtime/RepositoryWorkloadIT.java"))
                .as("RepositoryWorkloadIT.java should not be in the project directory in tests-only mode.")
                .doesNotExist();
        assertThat(outputDir.resolve("demo").toFile())
                .as("Generated project directory should be cleaned up after tests-only generation.")
                .doesNotExist();
    }

    @Test
    void testPostGenerate_TestsOnlyWithRelativeProjectDirectoryInProperties_ResolvesAgainstWorkingDir()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        Path workingDir = tempDir.resolve("workdir");
        Files.createDirectories(workingDir);
        String relativeProjectDir = "nested/project";

        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScript(outputDir, "demo", "tests-only", "none", relativeProjectDir,
                workingDir.toAbsolutePath().toString());

        assertThat(workingDir.resolve(relativeProjectDir)
                .resolve("src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("A relative projectDirectory must resolve against the working directory (PWD).")
                .exists();
    }

    @Test
    void testPostGenerate_TestsOnlyWithNoProjectDirectory_CopiesAuditFilesToOutputDir()
            throws Exception {
        Path outputDir = tempDir.resolve("output");

        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScript(outputDir, "demo", "tests-only", "none", null, null);

        assertThat(outputDir.resolve("src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("When projectDirectory is absent the files should land in the outputDirectory.")
                .exists();
        assertThat(outputDir.resolve("demo").toFile())
                .as("Generated project directory should be deleted when using outputDirectory as destination.")
                .doesNotExist();
    }

    @Test
    void testPostGenerate_TestsOnlyWithParentClass_DeletesAbstractBaseClass()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);

        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScript(outputDir, "demo", "tests-only", "some.ParentClass",
                projectDir.toAbsolutePath().toString(), null);

        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/AbstractDatabaseAuditIT.java"))
                .as("AbstractDatabaseAuditIT.java must be deleted when a parentClass is specified.")
                .doesNotExist();
        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("Other audit IT files should still be copied when a parentClass is specified.")
                .exists();
    }

    /**
     * Reproduces the reported command-line failure: {@code archetype:generate ... -DprojectDirectory=.
     * -DoutputDirectory=/scratch}. Because {@code projectDirectory} is not a declared archetype property,
     * the plugin never places it in {@code request.properties}; it is only a JVM system property. The
     * script must read it from there and resolve {@code '.'} against the working directory (PWD), so the
     * files land in the project directory and not in the scratch outputDirectory.
     */
    @Test
    void testPostGenerate_TestsOnlyWithDotProjectDirectoryAsSystemProperty_CopiesFilesToWorkingDir()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        Path workingDir = tempDir.resolve("project");
        Files.createDirectories(workingDir);

        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScriptWithSystemProjectDirectory(outputDir, "demo", "tests-only", "none",
                ".", workingDir.toAbsolutePath().toString());

        assertThat(workingDir.resolve("src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("projectDirectory='.' from a system property must resolve against PWD and land in the working directory.")
                .exists();
        assertThat(workingDir.resolve("src/test/resources/junit-platform.properties"))
                .as("junit-platform.properties should be copied to the working directory.")
                .exists();
        assertThat(outputDir.resolve("src").toFile())
                .as("Files must not be left behind in the scratch outputDirectory.")
                .doesNotExist();
        assertThat(outputDir.resolve("demo").toFile())
                .as("Generated project directory should be cleaned up after tests-only generation.")
                .doesNotExist();
    }

    @Test
    void testPostGenerate_TestsOnlyWithAbsoluteProjectDirectoryAsSystemProperty_CopiesFilesToProjectDir()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);

        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScriptWithSystemProjectDirectory(outputDir, "demo", "tests-only", "none",
                projectDir.toAbsolutePath().toString(), null);

        assertThat(projectDir.resolve("src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("An absolute projectDirectory from a system property must be used as-is.")
                .exists();
        assertThat(outputDir.resolve("src").toFile())
                .as("Files must not be left behind in the scratch outputDirectory.")
                .doesNotExist();
    }

    @Test
    void testPostGenerate_ProjectModeWithoutDataSourceNames_DeletesMultiTemplates()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);
        setupMultiTemplates(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScriptInProjectMode(outputDir, "demo", "none");

        assertThat(outputDir.resolve("demo/src/test/java/" + PACKAGE_PATH + "/multi").toFile())
                .as("The multi/ templates must be deleted when dataSourceNames is 'none'.")
                .doesNotExist();
        assertThat(outputDir.resolve("demo/src/test/java/" + PACKAGE_PATH + "/catalog/ForeignKeyIndexAuditIT.java"))
                .as("Audit IT files are kept in project mode.")
                .exists();
    }

    @Test
    void testPostGenerate_ProjectModeWithDataSourceNames_GeneratesNamedClassesPerDatasource()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);
        setupMultiTemplates(outputDir, "demo", PACKAGE_PATH);

        runPostGenerateScriptInProjectMode(outputDir, "demo", "Aurora, Reporting");

        Path multi = outputDir.resolve("demo/src/test/java/" + PACKAGE_PATH + "/multi");
        assertThat(multi.resolve("DatabaseAuditAuroraTestConfiguration.java"))
                .as("A config class is generated for each datasource name.").exists();
        assertThat(multi.resolve("DatabaseAuditReportingTestConfiguration.java"))
                .as("A config class is generated for each datasource name (whitespace is trimmed).").exists();
        assertThat(multi.resolve("DatabaseAuditDsNameTokenTestConfiguration.java").toFile())
                .as("The token template is removed after generation.").doesNotExist();
        assertThat(Files.readString(multi.resolve("DatabaseAuditAuroraTestConfiguration.java")))
                .as("Tokens are replaced with the datasource name throughout.")
                .contains("class DatabaseAuditAuroraTestConfiguration", "auroraDataSource")
                .doesNotContain("DsNameToken", "dsNameToken");
        assertThat(multi.resolve("DatabaseAuditMultiTestConfiguration.java"))
                .as("An aggregator config is generated to @Import every per-datasource config.").exists();
        assertThat(Files.readString(multi.resolve("DatabaseAuditMultiTestConfiguration.java")))
                .as("The aggregator @Imports every per-datasource config by its PascalCase class name.")
                .contains("class DatabaseAuditMultiTestConfiguration",
                        "@Import({DatabaseAuditAuroraTestConfiguration.class, "
                                + "DatabaseAuditReportingTestConfiguration.class})")
                .doesNotContain("DsNameToken");
    }

    @Test
    void testPostGenerate_ProjectModeWithInvalidDataSourceName_FailsWithClearError()
            throws Exception {
        Path outputDir = tempDir.resolve("output");
        setupGeneratedProject(outputDir, "demo", PACKAGE_PATH);
        setupMultiTemplates(outputDir, "demo", PACKAGE_PATH);

        assertThatThrownBy(() -> runPostGenerateScriptInProjectMode(outputDir, "demo",
                "read-replica"))
                .as("A datasource name that is not a valid Java identifier must fail generation, "
                        + "not emit an uncompilable class.")
                .hasMessageContaining("read-replica");
    }

    private void setupGeneratedProject(Path outputDir, String artifactId, String packagePath)
            throws IOException {
        Path projectDir = outputDir.resolve(artifactId);

        // Demo harness files — deleted by post-generate in tests-only mode
        Path demoApp = projectDir.resolve("src/test/java/" + packagePath + "/DemoApplication.java");
        Files.createDirectories(demoApp.getParent());
        Files.writeString(demoApp, "class DemoApplication {}");

        Path appDir = projectDir.resolve("src/test/java/" + packagePath + "/app");
        Files.createDirectories(appDir);
        Files.writeString(appDir.resolve("SomeEntity.java"), "class SomeEntity {}");

        Path runtimeDir = projectDir.resolve("src/test/java/" + packagePath + "/runtime");
        Files.createDirectories(runtimeDir);
        Files.writeString(runtimeDir.resolve("RepositoryWorkloadIT.java"), "class RepositoryWorkloadIT {}");

        Path resourcesDir = projectDir.resolve("src/test/resources");
        Files.createDirectories(resourcesDir);
        Files.writeString(resourcesDir.resolve("application.properties"), "# app");
        Path dbDir = resourcesDir.resolve("db/changelog");
        Files.createDirectories(dbDir);
        Files.writeString(dbDir.resolve("master.xml"), "<databaseChangelog/>");

        // Audit IT files — kept and copied by post-generate
        Path catalogDir = projectDir.resolve("src/test/java/" + packagePath + "/catalog");
        Files.createDirectories(catalogDir);
        Files.writeString(catalogDir.resolve("ForeignKeyIndexAuditIT.java"), "class ForeignKeyIndexAuditIT {}");

        Path baseClass = projectDir.resolve("src/test/java/" + packagePath + "/AbstractDatabaseAuditIT.java");
        Files.writeString(baseClass, "class AbstractDatabaseAuditIT {}");

        Files.writeString(resourcesDir.resolve("junit-platform.properties"), "# junit");
        Files.writeString(projectDir.resolve("pom.xml"), "<project/>");
    }

    /**
     * Runs the post-generate script with {@code projectDirectory} supplied through
     * {@code request.properties} (the archetype integration-test mojo's behavior). A {@code null}
     * {@code requestProjectDirectory} omits the property entirely, mirroring {@code archetype:generate}.
     * An optional {@code pwdOverride} is injected as {@code _pwd} to stand in for {@code System.getenv("PWD")}.
     */
    private void runPostGenerateScript(Path outputDir, String artifactId, String generateMode,
            String parentClass, String requestProjectDirectory, String pwdOverride) throws IOException {
        Properties props = new Properties();
        props.setProperty("generateMode", generateMode);
        props.setProperty("parentClass", parentClass);
        props.setProperty("package", PACKAGE);
        if (requestProjectDirectory != null) {
            props.setProperty("projectDirectory", requestProjectDirectory);
        }
        evaluateScript(outputDir, artifactId, props, pwdOverride);
    }

    /**
     * Runs the post-generate script with {@code projectDirectory} supplied only as a JVM system
     * property and deliberately absent from {@code request.properties} — exactly how it arrives from the
     * {@code archetype:generate} command line. The system property is restored afterward.
     */
    private void runPostGenerateScriptWithSystemProjectDirectory(Path outputDir, String artifactId,
            String generateMode, String parentClass, String projectDirectorySysProp, String pwdOverride)
            throws IOException {
        String previous = System.getProperty("projectDirectory");
        System.setProperty("projectDirectory", projectDirectorySysProp);
        try {
            runPostGenerateScript(outputDir, artifactId, generateMode, parentClass, null, pwdOverride);
        } finally {
            if (previous == null) {
                System.clearProperty("projectDirectory");
            } else {
                System.setProperty("projectDirectory", previous);
            }
        }
    }

    private void setupMultiTemplates(Path outputDir, String artifactId, String packagePath)
            throws IOException {
        Path multiDir = outputDir.resolve(artifactId).resolve("src/test/java/" + packagePath + "/multi");
        Files.createDirectories(multiDir);
        Files.writeString(multiDir.resolve("DatabaseAuditDsNameTokenTestConfiguration.java"),
                "class DatabaseAuditDsNameTokenTestConfiguration { String ds = \"dsNameTokenDataSource\"; }");
    }

    /**
     * Runs the post-generate script in {@code project} mode, where the only cleanup is the {@code parentClass}
     * deletion and the {@code dataSourceNames} expansion. A {@code null} {@code dataSourceNames} omits the
     * property entirely, mirroring no datasource names.
     */
    private void runPostGenerateScriptInProjectMode(Path outputDir, String artifactId, String dataSourceNames)
            throws IOException {
        Properties props = new Properties();
        props.setProperty("generateMode", "project");
        props.setProperty("parentClass", "none");
        props.setProperty("package", PACKAGE);
        if (dataSourceNames != null) {
            props.setProperty("dataSourceNames", dataSourceNames);
        }
        evaluateScript(outputDir, artifactId, props, null);
    }

    private void evaluateScript(Path outputDir, String artifactId, Properties props, String pwdOverride)
            throws IOException {
        String script;
        try (InputStream is = getClass().getResourceAsStream("/META-INF/archetype-post-generate.groovy")) {
            assertThat(is).as("Post-generate script must be on the test classpath.").isNotNull();
            script = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Binding binding = new Binding();
        binding.setVariable("request",
                new MockRequest(props, outputDir.toAbsolutePath().toString(), artifactId));
        if (pwdOverride != null) {
            binding.setVariable("_pwd", pwdOverride);
        }
        new GroovyShell(binding).evaluate(script);
    }

    /**
     * Minimal stand-in for the Maven archetype {@code ArchetypeGenerationRequest} — exposes only
     * the properties, outputDirectory, and artifactId fields the post-generate script reads.
     */
    static final class MockRequest {
        private final Properties properties;
        private final String outputDirectory;
        private final String artifactId;

        MockRequest(Properties properties, String outputDirectory, String artifactId) {
            this.properties = properties;
            this.outputDirectory = outputDirectory;
            this.artifactId = artifactId;
        }

        public Properties getProperties() {
            return properties;
        }

        public String getOutputDirectory() {
            return outputDirectory;
        }

        public String getArtifactId() {
            return artifactId;
        }
    }
}
