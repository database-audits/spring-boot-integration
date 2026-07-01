package ${package};

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.DatabaseAuditSuite;
import io.github.databaseaudits.spring.boot.SqlCapturerRegisteringPostProcessor;
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
 * {@code DatabaseAuditTestConfiguration}, which resolves the primary datasource by type — ambiguous when an
 * application has several peer datasources; this configuration resolves the DsNameToken datasource by name instead,
 * so no datasource needs to be {@code @Primary}. The test base class {@code @Import}s it; every audit IT extends
 * that base.
 *
 * <p>
 * To point it at your datasource, the {@code @Qualifier} values are filled in at generation from the
 * {@code dataSourceBeanName} and {@code entityManagerFactoryBeanName} archetype properties; edit them here if your
 * bean names change. The runtime (PostgreSQL-only) audits need the DsNameToken datasource's SQL captured, and this
 * configuration wires that automatically: it registers a {@link SqlCapturerRegisteringPostProcessor} that sets
 * {@code dsNameTokenSqlCapturer} as that {@code EntityManagerFactory}'s Hibernate {@code StatementInspector} before
 * the factory is built — no change to the application's own configuration of that factory. You need only connect the
 * datasource with {@code preferQueryMode=simple} for the plan-based audits. (The auto-wiring applies when the factory
 * bean is a {@code LocalContainerEntityManagerFactoryBean}, the usual case; otherwise set the inspector where you
 * build the factory, or run only the catalog and JPA audits against it.)
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
     * Registers {@link #dsNameTokenSqlCapturer()} as the DsNameToken {@code EntityManagerFactory}'s Hibernate
     * {@code StatementInspector} before that factory is built, so the runtime audits capture its SQL with no change
     * to the application's own configuration of that factory. {@code static} so it is registered early enough to
     * post-process the factory bean.
     *
     * @return the post-processor that wires the capturer into the DsNameToken entity-manager factory.
     */
    @Bean
    static SqlCapturerRegisteringPostProcessor dsNameTokenSqlCapturerRegisteringPostProcessor() {
        return new SqlCapturerRegisteringPostProcessor("${entityManagerFactoryBeanName}", "dsNameTokenSqlCapturer");
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
