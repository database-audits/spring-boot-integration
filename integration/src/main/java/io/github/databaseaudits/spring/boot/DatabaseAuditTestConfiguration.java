package io.github.databaseaudits.spring.boot;

import javax.sql.DataSource;

import org.hibernate.cfg.JdbcSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.databaseaudits.audit.catalog.ForeignKeyIndexAudit;
import io.github.databaseaudits.audit.catalog.ForeignKeyNotNullAudit;
import io.github.databaseaudits.audit.catalog.ForeignKeyTypeMatchAudit;
import io.github.databaseaudits.audit.catalog.PrimaryKeyPresenceAudit;
import io.github.databaseaudits.audit.catalog.RedundantIndexAudit;
import io.github.databaseaudits.audit.jpa.SchemaEntityValidationAudit;
import io.github.databaseaudits.audit.runtime.UnconditionalMutationAudit;
import io.github.databaseaudits.audit.runtime.plan.JoinIndexAudit;
import io.github.databaseaudits.audit.runtime.plan.OrderByIndexAudit;
import io.github.databaseaudits.audit.runtime.plan.WhereClauseIndexAudit;
import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.catalog.IndexCatalog;
import io.github.databaseaudits.jdbc.CatalogQueries;
import io.github.databaseaudits.plan.QueryPlanExplainer;
import io.github.databaseaudits.platform.DatabasePlatform;
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
 * Wires the database-audit suite for a module's integration-test context. The
 * core module is dependency-injection-framework-neutral (its classes carry no
 * Spring annotations), so this configuration registers every audit, the
 * {@link QueryPlanExplainer}, and the {@link SqlCapturingStatementInspector} as
 * explicit beans, and registers the capturer as Hibernate's
 * {@code StatementInspector} so the runtime audits see every statement the
 * repositories run.
 *
 * <p>
 * A single {@code @Import(DatabaseAuditTestConfiguration.class)} on your
 * test-infrastructure base class is enough — no separate component-scan setup
 * required. (Assumes a JPA + Liquibase + DataSource context, which the DB
 * integration tests already boot.)
 *
 * <p>
 * The {@link DatabasePlatform} is auto-detected from the {@link DataSource}
 * metadata, so the audits run the catalog SQL matching the database under test.
 * The plan-based audits ({@link WhereClauseIndexAudit},
 * {@link OrderByIndexAudit}, {@link JoinIndexAudit}) are PostgreSQL-only and
 * fail fast on other platforms. For them the test {@link DataSource} must also
 * connect with {@code preferQueryMode=simple} (a PostgreSQL JDBC connection
 * property, e.g. appended to the JDBC URL): generic-plan EXPLAIN of a statement
 * containing {@code $n} placeholders only works over the simple query protocol
 * — see {@link QueryPlanExplainer}. Without it every parameterized statement is
 * skipped, and the plan audits then fail their vacuous-run guard.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DatabaseAuditTestConfiguration {
    @Bean
    SqlCapturingStatementInspector sqlCapturingStatementInspector() {
        return new SqlCapturingStatementInspector();
    }

    @Bean
    HibernatePropertiesCustomizer sqlCaptureHibernatePropertiesCustomizer(
            final SqlCapturingStatementInspector sqlCapturer) {
        // Register the Spring bean itself, so the inspector and the injected
        // audits share one capture instance.
        return properties -> properties.put(JdbcSettings.STATEMENT_INSPECTOR,
                sqlCapturer);
    }

    @Bean
    DatabasePlatform databasePlatform(final DataSource dataSource) {
        return DatabasePlatform.fromDataSource(dataSource);
    }

    @Bean
    CatalogQueries jdbcSupport(final DataSource dataSource) {
        return new CatalogQueries(dataSource);
    }

    @Bean
    IndexCatalog indexCatalog(final CatalogQueries jdbcSupport,
            final DatabasePlatform platform) {
        return new IndexCatalog(jdbcSupport, platform);
    }

    /**
     * The explainer behind the plan-based audits. On PostgreSQL the injected
     * {@link DataSource} must connect with {@code preferQueryMode=simple} — see
     * the {@link QueryPlanExplainer} class contract.
     */
    @Bean
    QueryPlanExplainer queryPlanExplainer(final DataSource dataSource,
            final DatabasePlatform platform) {
        return new QueryPlanExplainer(dataSource, platform);
    }

    @Bean
    WhereClauseIndexAudit whereClauseIndexAudit(
            final QueryPlanExplainer queryPlanExplainer,
            final SqlCapturingStatementInspector sqlCapturer) {
        return new WhereClauseIndexAudit(queryPlanExplainer, sqlCapturer);
    }

    @Bean
    OrderByIndexAudit orderByIndexAudit(
            final QueryPlanExplainer queryPlanExplainer,
            final SqlCapturingStatementInspector sqlCapturer) {
        return new OrderByIndexAudit(queryPlanExplainer, sqlCapturer);
    }

    @Bean
    JoinIndexAudit joinIndexAudit(final QueryPlanExplainer queryPlanExplainer,
            final SqlCapturingStatementInspector sqlCapturer) {
        return new JoinIndexAudit(queryPlanExplainer, sqlCapturer);
    }

    @Bean
    UnconditionalMutationAudit unconditionalMutationAudit(
            final SqlCapturingStatementInspector sqlCapturer) {
        return new UnconditionalMutationAudit(sqlCapturer);
    }

    @Bean
    PrimaryKeyPresenceAudit primaryKeyPresenceAudit(
            final CatalogQueries jdbcSupport, final DatabasePlatform platform) {
        return new PrimaryKeyPresenceAudit(jdbcSupport, platform);
    }

    @Bean
    ForeignKeyIndexAudit foreignKeyIndexAudit(final CatalogQueries jdbcSupport,
            final IndexCatalog indexCatalog, final DatabasePlatform platform) {
        return new ForeignKeyIndexAudit(jdbcSupport, indexCatalog, platform);
    }

    @Bean
    ForeignKeyNotNullAudit foreignKeyNotNullAudit(
            final CatalogQueries jdbcSupport, final DatabasePlatform platform) {
        return new ForeignKeyNotNullAudit(jdbcSupport, platform);
    }

    @Bean
    ForeignKeyTypeMatchAudit foreignKeyTypeMatchAudit(
            final CatalogQueries jdbcSupport, final DatabasePlatform platform) {
        return new ForeignKeyTypeMatchAudit(jdbcSupport, platform);
    }

    @Bean
    RedundantIndexAudit redundantIndexAudit(final IndexCatalog indexCatalog) {
        return new RedundantIndexAudit(indexCatalog);
    }

    @Bean
    SchemaEntityValidationAudit schemaEntityValidationAudit(
            final EntityManagerFactory entityManagerFactory) {
        return new SchemaEntityValidationAudit(entityManagerFactory);
    }

    @Bean
    ForeignKeyIndexAuditAssertion foreignKeyIndexAuditAssertion(
            final ForeignKeyIndexAudit foreignKeyIndexAudit) {
        return new ForeignKeyIndexAuditAssertion(foreignKeyIndexAudit);
    }

    @Bean
    ForeignKeyNotNullAuditAssertion foreignKeyNotNullAuditAssertion(
            final ForeignKeyNotNullAudit foreignKeyNotNullAudit) {
        return new ForeignKeyNotNullAuditAssertion(foreignKeyNotNullAudit);
    }

    @Bean
    ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatchAuditAssertion(
            final ForeignKeyTypeMatchAudit foreignKeyTypeMatchAudit) {
        return new ForeignKeyTypeMatchAuditAssertion(foreignKeyTypeMatchAudit);
    }

    @Bean
    PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion(
            final PrimaryKeyPresenceAudit primaryKeyPresenceAudit) {
        return new PrimaryKeyPresenceAuditAssertion(primaryKeyPresenceAudit);
    }

    @Bean
    RedundantIndexAuditAssertion redundantIndexAuditAssertion(
            final RedundantIndexAudit redundantIndexAudit) {
        return new RedundantIndexAuditAssertion(redundantIndexAudit);
    }

    @Bean
    SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion(
            final SchemaEntityValidationAudit schemaEntityValidationAudit) {
        return new SchemaEntityValidationAuditAssertion(
                schemaEntityValidationAudit);
    }

    @Bean
    JoinIndexAuditAssertion joinIndexAuditAssertion(
            final JoinIndexAudit joinIndexAudit) {
        return new JoinIndexAuditAssertion(joinIndexAudit);
    }

    @Bean
    OrderByIndexAuditAssertion orderByIndexAuditAssertion(
            final OrderByIndexAudit orderByIndexAudit) {
        return new OrderByIndexAuditAssertion(orderByIndexAudit);
    }

    @Bean
    WhereClauseIndexAuditAssertion whereClauseIndexAuditAssertion(
            final WhereClauseIndexAudit whereClauseIndexAudit) {
        return new WhereClauseIndexAuditAssertion(whereClauseIndexAudit);
    }

    @Bean
    UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion(
            final UnconditionalMutationAudit unconditionalMutationAudit) {
        return new UnconditionalMutationAuditAssertion(
                unconditionalMutationAudit);
    }

    @Bean
    DatabaseAuditAssertions databaseAuditAssertions(
            final ForeignKeyIndexAuditAssertion foreignKeyIndexAuditAssertion,
            final ForeignKeyNotNullAuditAssertion foreignKeyNotNullAuditAssertion,
            final ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatchAuditAssertion,
            final PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion,
            final RedundantIndexAuditAssertion redundantIndexAuditAssertion,
            final SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion,
            final JoinIndexAuditAssertion joinIndexAuditAssertion,
            final OrderByIndexAuditAssertion orderByIndexAuditAssertion,
            final WhereClauseIndexAuditAssertion whereClauseIndexAuditAssertion,
            final UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion) {
        return new DatabaseAuditAssertions(foreignKeyIndexAuditAssertion,
                foreignKeyNotNullAuditAssertion,
                foreignKeyTypeMatchAuditAssertion,
                primaryKeyPresenceAuditAssertion, redundantIndexAuditAssertion,
                schemaEntityValidationAuditAssertion, joinIndexAuditAssertion,
                orderByIndexAuditAssertion, whereClauseIndexAuditAssertion,
                unconditionalMutationAuditAssertion);
    }
}
