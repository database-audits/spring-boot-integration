package ${package}.multi;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.DatabaseAuditSuite;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;
import jakarta.persistence.EntityManagerFactory;

/**
 * Audits the DsNameToken datasource. The default {@code DatabaseAuditTestConfiguration} (imported by
 * {@code AbstractDatabaseAuditIT}) audits the application's primary {@code DataSource} and
 * {@code EntityManagerFactory}; this configuration audits the DsNameToken datasource, selected by
 * {@code @Qualifier}.
 *
 * <p>
 * To use it: replace the placeholder {@code @Qualifier} values with your DsNameToken {@code DataSource} and
 * {@code EntityManagerFactory} bean names, {@code @Import} this class from a test, and inject the
 * {@code dsNameTokenDatabaseAuditAssertions} bean (see {@code DatabaseAuditDsNameTokenIT}).
 *
 * <p>
 * It is a {@code @TestConfiguration}, so it stays inert until imported: the placeholder qualifiers never resolve
 * and the build still compiles. For the runtime (PostgreSQL-only) audits to see SQL, register
 * {@code dsNameTokenSqlCapturer} as that {@code EntityManagerFactory}'s Hibernate {@code StatementInspector} where
 * you build it, and connect that datasource with {@code preferQueryMode=simple}; otherwise call only
 * {@code assertCatalogClean} and {@code assertJpaClean}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DatabaseAuditDsNameTokenTestConfiguration {
    /**
     * The SQL capturer the DsNameToken datasource's runtime audits read. Must be the same instance registered as
     * that {@code EntityManagerFactory}'s Hibernate {@code StatementInspector}.
     *
     * @return the DsNameToken datasource's SQL capturer.
     */
    @Bean
    SqlCapturingStatementInspector dsNameTokenSqlCapturer() {
        return new SqlCapturingStatementInspector();
    }

    /**
     * Builds the audit assertions for the DsNameToken datasource. Replace the placeholder {@code @Qualifier}
     * values with your own bean names.
     *
     * @param dataSource
     *                                the DsNameToken datasource to audit.
     * @param entityManagerFactory
     *                                the DsNameToken entity-manager factory the JPA audit confirms.
     * @param dsNameTokenSqlCapturer
     *                                the DsNameToken datasource's SQL capturer.
     * @return the DsNameToken datasource's assertions facade.
     */
    @Bean
    DatabaseAuditAssertions dsNameTokenDatabaseAuditAssertions(
            @Qualifier("dsNameTokenDataSource") DataSource dataSource,
            @Qualifier("dsNameTokenEntityManagerFactory") EntityManagerFactory entityManagerFactory,
            @Qualifier("dsNameTokenSqlCapturer") SqlCapturingStatementInspector dsNameTokenSqlCapturer) {
        return new DatabaseAuditSuite(dataSource, entityManagerFactory, dsNameTokenSqlCapturer).assertions();
    }
}
