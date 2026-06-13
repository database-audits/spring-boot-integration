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
import io.github.databaseaudits.catalog.IndexCatalog;
import io.github.databaseaudits.jdbc.CatalogQueries;
import io.github.databaseaudits.platform.DatabasePlatform;
import jakarta.persistence.EntityManagerFactory;

/**
 * Integration of the Spring wiring with the core capturer and Hibernate's
 * settings contract — the configuration, the inspector, and the
 * {@code HibernatePropertiesCustomizer} collaborating. No Spring context is
 * booted.
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

    @Test
    void testDatabasePlatform_DetectsFromTheDataSourceMetadata()
            throws SQLException {
        final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        final Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metaData);
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);

        assertThat(configuration.databasePlatform(dataSource))
                .as("The platform is detected from the DataSource metadata.")
                .isEqualTo(DatabasePlatform.POSTGRESQL);
    }

    /**
     * Every audit bean method constructs — proves the wiring compiles against
     * core's constructors.
     */
    @Test
    void testEveryBeanMethodConstructsItsBean() {
        final DataSource dataSource = mock(DataSource.class);
        final CatalogQueries jdbcSupport = mock(CatalogQueries.class);
        final IndexCatalog indexCatalog = mock(IndexCatalog.class);
        final SqlCapturingStatementInspector capturer =
                configuration.sqlCapturingStatementInspector();
        final DatabasePlatform platform = DatabasePlatform.POSTGRESQL;
        final var queryPlanExplainer =
                configuration.queryPlanExplainer(dataSource, platform);

        assertThat(configuration.jdbcSupport(dataSource))
                .as("The jdbcSupport bean constructs.").isNotNull();
        assertThat(configuration.indexCatalog(jdbcSupport, platform))
                .as("The indexCatalog bean constructs.").isNotNull();
        assertThat(queryPlanExplainer)
                .as("The queryPlanExplainer bean constructs.").isNotNull();
        assertThat(configuration.whereClauseIndexAudit(queryPlanExplainer,
                capturer)).as("The whereClauseIndexAudit bean constructs.")
                .isNotNull();
        assertThat(
                configuration.orderByIndexAudit(queryPlanExplainer, capturer))
                .as("The orderByIndexAudit bean constructs.").isNotNull();
        assertThat(configuration.joinIndexAudit(queryPlanExplainer, capturer))
                .as("The joinIndexAudit bean constructs.").isNotNull();
        assertThat(configuration.unconditionalMutationAudit(capturer))
                .as("The unconditionalMutationAudit bean constructs.")
                .isNotNull();
        assertThat(configuration.primaryKeyPresenceAudit(jdbcSupport, platform))
                .as("The primaryKeyPresenceAudit bean constructs.").isNotNull();
        assertThat(configuration.foreignKeyIndexAudit(jdbcSupport, indexCatalog,
                platform)).as("The foreignKeyIndexAudit bean constructs.")
                .isNotNull();
        assertThat(configuration.foreignKeyNotNullAudit(jdbcSupport, platform))
                .as("The foreignKeyNotNullAudit bean constructs.").isNotNull();
        assertThat(
                configuration.foreignKeyTypeMatchAudit(jdbcSupport, platform))
                .as("The foreignKeyTypeMatchAudit bean constructs.")
                .isNotNull();
        assertThat(configuration.redundantIndexAudit(indexCatalog))
                .as("The redundantIndexAudit bean constructs.").isNotNull();
        assertThat(configuration
                .schemaEntityValidationAudit(mock(EntityManagerFactory.class)))
                .as("The schemaEntityValidationAudit bean constructs.")
                .isNotNull();
    }
}
