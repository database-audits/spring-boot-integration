package ${package};

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.DatabaseAuditSuite;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyNotNullAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyTypeMatchAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.JoinIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.OrderByIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.PrimaryKeyPresenceAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.RedundantIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.SchemaEntityValidationAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.UnconditionalMutationAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.WhereClauseIndexAuditAssertion;
import jakarta.persistence.EntityManagerFactory;

/**
 * Wires the audit suite for the DsNameToken datasource, selected by {@code @Qualifier}, and registers every
 * {@code *AuditAssertion} bean the audit ITs {@code @Autowired}. It stands in for the stock
 * {@code DatabaseAuditTestConfiguration}, which audits the primary datasource by type and backs off when an
 * application has several peer datasources; this configuration resolves the DsNameToken datasource by name instead,
 * so no datasource needs to be {@code @Primary}. Each audit IT {@code @Import}s it.
 *
 * <p>
 * To point it at your datasource, the {@code @Qualifier} values are filled in at generation from the
 * {@code dataSourceBeanName} and {@code entityManagerFactoryBeanName} archetype properties; edit them here if your
 * bean names change. For the runtime (PostgreSQL-only) audits to see SQL, register {@code dsNameTokenSqlCapturer}
 * as that {@code EntityManagerFactory}'s Hibernate {@code StatementInspector} where you build it, and connect the
 * datasource with {@code preferQueryMode=simple}; otherwise run only the catalog and JPA audits against it.
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
     * Builds the audit suite for the DsNameToken datasource. The {@code @Qualifier} values are filled in at
     * generation from the {@code dataSourceBeanName} and {@code entityManagerFactoryBeanName} archetype properties.
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
            @Qualifier("${dataSourceBeanName}") DataSource dataSource,
            @Qualifier("${entityManagerFactoryBeanName}") EntityManagerFactory entityManagerFactory,
            @Qualifier("dsNameTokenSqlCapturer") SqlCapturingStatementInspector dsNameTokenSqlCapturer) {
        return new DatabaseAuditSuite(dataSource, entityManagerFactory, dsNameTokenSqlCapturer);
    }

    @Bean
    ForeignKeyIndexAuditAssertion foreignKeyIndexAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.foreignKeyIndexAuditAssertion();
    }

    @Bean
    ForeignKeyNotNullAuditAssertion foreignKeyNotNullAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.foreignKeyNotNullAuditAssertion();
    }

    @Bean
    ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatchAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.foreignKeyTypeMatchAuditAssertion();
    }

    @Bean
    PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.primaryKeyPresenceAuditAssertion();
    }

    @Bean
    RedundantIndexAuditAssertion redundantIndexAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.redundantIndexAuditAssertion();
    }

    @Bean
    SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.schemaEntityValidationAuditAssertion();
    }

    @Bean
    JoinIndexAuditAssertion joinIndexAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.joinIndexAuditAssertion();
    }

    @Bean
    OrderByIndexAuditAssertion orderByIndexAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.orderByIndexAuditAssertion();
    }

    @Bean
    WhereClauseIndexAuditAssertion whereClauseIndexAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.whereClauseIndexAuditAssertion();
    }

    @Bean
    UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion(final DatabaseAuditSuite suite) {
        return suite.unconditionalMutationAuditAssertion();
    }

    @Bean
    DatabaseAuditAssertions databaseAuditAssertions(final DatabaseAuditSuite suite) {
        return suite.assertions();
    }
}
