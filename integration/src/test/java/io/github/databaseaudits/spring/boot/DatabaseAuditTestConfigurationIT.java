package io.github.databaseaudits.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.cfg.JdbcSettings;
import org.junit.jupiter.api.Test;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
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
