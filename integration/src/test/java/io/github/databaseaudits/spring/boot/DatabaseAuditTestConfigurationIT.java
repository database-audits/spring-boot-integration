package io.github.databaseaudits.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hibernate.cfg.JdbcSettings;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.assertion.AuditAssertion;
import jakarta.persistence.EntityManagerFactory;

/**
 * Integration of the Spring wiring with the core capturer and Hibernate's
 * settings contract — the configuration, the {@link DatabaseAuditSuite}, the
 * inspector, and the {@code HibernatePropertiesCustomizer} collaborating. No
 * Spring context is booted.
 */
class DatabaseAuditTestConfigurationIT {
    private final DatabaseAuditTestConfiguration configuration =
            new DatabaseAuditTestConfiguration();

    /**
     * Verify Hibernate has the inspector bean instance itself — never a class
     * name, which would capture into a second instance the audits never see.
     */
    @Test
    void testSqlCaptureHibernatePropertiesCustomizer_RegistersTheSameInspectorInstanceWithHibernate() {
        final SqlCapturingStatementInspector inspector =
                configuration.sqlCapturingStatementInspector();
        final Map<String, Object> properties = new HashMap<>();

        configuration.sqlCaptureHibernatePropertiesCustomizer(inspector)
                .customize(properties);

        final Object actual = properties.get(JdbcSettings.STATEMENT_INSPECTOR);
        assertThat(actual).as(
                "Hibernate receives the same inspector bean the audits read from.")
                .isSameAs(inspector);
    }

    /**
     * Every assertion bean method delegates to the suite, which constructs the
     * whole graph — proving the wiring compiles and runs against core's audit
     * constructors and that the platform is detected from the
     * {@link DataSource}.
     */
    @Test
    void testEveryAssertionBeanMethod_DelegatesToTheConstructedSuite()
            throws SQLException {
        final DataSource dataSource = postgresDataSource();
        final SqlCapturingStatementInspector capturer =
                configuration.sqlCapturingStatementInspector();
        final DatabaseAuditSuite suite = configuration.databaseAuditSuite(
                dataSource, mock(EntityManagerFactory.class), capturer);

        assertThat(suite)
                .as("The suite bean constructs against core's audit constructors.")
                .isNotNull();
        assertThat(configuration.foreignKeyIndexAuditAssertion(suite))
                .as("The foreignKeyIndexAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.foreignKeyNotNullAuditAssertion(suite))
                .as("The foreignKeyNotNullAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.foreignKeyTypeMatchAuditAssertion(suite))
                .as("The foreignKeyTypeMatchAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.primaryKeyPresenceAuditAssertion(suite))
                .as("The primaryKeyPresenceAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.redundantIndexAuditAssertion(suite))
                .as("The redundantIndexAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.schemaEntityValidationAuditAssertion(suite))
                .as("The schemaEntityValidationAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.joinIndexAuditAssertion(suite))
                .as("The joinIndexAuditAssertion bean constructs.").isNotNull();
        assertThat(configuration.orderByIndexAuditAssertion(suite))
                .as("The orderByIndexAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.whereClauseIndexAuditAssertion(suite))
                .as("The whereClauseIndexAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.unconditionalMutationAuditAssertion(suite))
                .as("The unconditionalMutationAuditAssertion bean constructs.")
                .isNotNull();
        assertThat(configuration.databaseAuditAssertions(suite))
                .as("The databaseAuditAssertions facade bean constructs.")
                .isNotNull();
    }

    /**
     * Guards the roster: every concrete {@code *Audit} in core must have a
     * wired {@code *AuditAssertion} in the suite, so adding a core audit without
     * wiring it here fails the build instead of silently never running.
     */
    @Test
    void testSuiteAll_WiresAnAssertionForEveryCoreAudit()
            throws SQLException {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(
                new RegexPatternTypeFilter(Pattern.compile(".*Audit")));
        final Set<String> coreAudits = scanner
                .findCandidateComponents("io.github.databaseaudits.audit")
                .stream()
                .map(definition -> simpleName(definition.getBeanClassName()))
                .collect(Collectors.toSet());

        final DatabaseAuditSuite suite = new DatabaseAuditSuite(
                postgresDataSource(), mock(EntityManagerFactory.class),
                new SqlCapturingStatementInspector());
        final Set<String> wiredAudits = suite.all().stream()
                .map(DatabaseAuditTestConfigurationIT::auditName)
                .collect(Collectors.toSet());

        assertThat(wiredAudits).as(
                "Every core *Audit has a wired *AuditAssertion in the suite.")
                .isEqualTo(coreAudits);
    }

    private static String simpleName(final String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static String auditName(final AuditAssertion assertion) {
        final String name = assertion.getClass().getSimpleName();
        return name.substring(0, name.length() - "Assertion".length());
    }

    private static DataSource postgresDataSource() throws SQLException {
        final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        final Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metaData);
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}
