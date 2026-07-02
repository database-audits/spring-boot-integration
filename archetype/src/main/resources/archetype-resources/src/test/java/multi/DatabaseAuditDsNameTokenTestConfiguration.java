package ${package}.multi;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.DatabaseAuditSuite;
import jakarta.persistence.EntityManagerFactory;

/**
 * Wires the audit suite for the DsNameToken datasource, selected by {@code @Qualifier}. It exposes a
 * {@link DatabaseAuditSuite} bean named {@code dsNameTokenDatabaseAuditSuite}; the parameterized audit ITs look it
 * up by that name (their {@code @ValueSource} carries the datasource name and appends {@code DatabaseAuditSuite}).
 * {@link DatabaseAuditMultiTestConfiguration} aggregates this and every other per-datasource config behind one
 * {@code @Import}.
 *
 * <p>
 * To use it: replace the placeholder {@code @Qualifier} values with your DsNameToken {@code DataSource} and
 * {@code EntityManagerFactory} bean names. No datasource is assumed {@code @Primary} — every datasource, including
 * this one, is resolved by name, so an application with several peer {@code EntityManagerFactory} beans needs none
 * of them marked {@code @Primary}.
 *
 * <p>
 * It is a {@code @TestConfiguration}, so it stays inert until imported: the placeholder qualifiers never resolve
 * and the build still compiles. For the runtime (PostgreSQL-only) audits to see SQL, register
 * {@code dsNameTokenSqlCapturer} as that {@code EntityManagerFactory}'s Hibernate {@code StatementInspector} where
 * you build it, and connect that datasource with {@code preferQueryMode=simple}; otherwise run only the catalog
 * and JPA audits against this datasource.
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
     * Builds the audit suite for the DsNameToken datasource. Replace the placeholder {@code @Qualifier} values
     * with your own bean names.
     *
     * @param dataSource
     *                                the DsNameToken datasource to audit.
     * @param entityManagerFactory
     *                                the DsNameToken entity-manager factory the JPA audit confirms.
     * @param dsNameTokenSqlCapturer
     *                                the DsNameToken datasource's SQL capturer.
     * @return the DsNameToken datasource's audit suite.
     */
    @Bean
    DatabaseAuditSuite dsNameTokenDatabaseAuditSuite(
            @Qualifier("dsNameTokenDataSource") DataSource dataSource,
            @Qualifier("dsNameTokenEntityManagerFactory") EntityManagerFactory entityManagerFactory,
            @Qualifier("dsNameTokenSqlCapturer") SqlCapturingStatementInspector dsNameTokenSqlCapturer) {
        return new DatabaseAuditSuite(dataSource, entityManagerFactory, dsNameTokenSqlCapturer);
    }
}
