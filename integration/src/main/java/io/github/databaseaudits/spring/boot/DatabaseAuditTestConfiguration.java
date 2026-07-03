package io.github.databaseaudits.spring.boot;

import javax.sql.DataSource;

import org.hibernate.cfg.JdbcSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;
import jakarta.persistence.EntityManagerFactory;

/**
 * Wires the database-audit suite for a module's integration-test context. The
 * core module is dependency-injection-framework-neutral (its classes carry no
 * Spring annotations), so this configuration builds a {@link DatabaseAuditSuite}
 * from the context's primary {@link DataSource} and {@link EntityManagerFactory}
 * and, through an {@link AuditAssertionRegistrar}, registers its
 * {@code *AuditAssertion}s — and the {@link DatabaseAuditAssertions} facade — as
 * beans. It also registers the suite's {@link SqlCapturingStatementInspector} as
 * Hibernate's {@code StatementInspector}, so the runtime audits see every
 * statement the repositories run.
 *
 * <p>
 * A single {@code @Import(DatabaseAuditTestConfiguration.class)} on your
 * test-infrastructure base class is enough — no separate component-scan setup
 * required. (Assumes a JPA + Liquibase + DataSource context, which the DB
 * integration tests already boot.) The platform is auto-detected from the
 * {@link DataSource}; the PostgreSQL-only requirements of the plan-based audits
 * (including the {@code preferQueryMode=simple} connection property) are
 * described on {@link DatabaseAuditSuite}.
 *
 * <p>
 * This is the default, single-datasource entry point: it audits <em>one</em>
 * datasource, resolving the {@link DataSource}/{@link EntityManagerFactory}
 * <em>by type</em> — Spring's autowiring selects the single, {@code @Primary}, or
 * conventionally-named ({@code dataSource}/{@code entityManagerFactory}) candidate.
 * Several peer datasources it cannot disambiguate make importing this configuration
 * fail fast with Spring's "expected single matching bean but found 2". To audit a
 * <em>specific</em> datasource <em>by name</em> instead (typically a non-primary one
 * among peers), use the archetype-generated {@code DatabaseAudit<Name>TestConfiguration}
 * — a {@code @Qualifier}-based mirror of this class — rather than this one. That does
 * not make this class obsolete: it stays the entry point for auditing any single or
 * {@code @Primary} datasource. See the integration usage docs.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DatabaseAuditTestConfiguration {
    /**
     * Creates the configuration; the audit beans are registered by the
     * {@code @Bean} methods and the {@link AuditAssertionRegistrar}.
     */
    public DatabaseAuditTestConfiguration() {
    }

    /**
     * The SQL capturer the runtime audits read. Marked {@code @Primary} so this
     * configuration's by-type injections of it still resolve when an application
     * adds a second capturer for another datasource's suite.
     *
     * @return the shared SQL capturer.
     */
    @Bean
    @Primary
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

    /**
     * The audit suite for the context's primary datasource. On PostgreSQL the
     * injected {@link DataSource} must connect with {@code preferQueryMode=simple}
     * — see {@link DatabaseAuditSuite}. Marked {@code @Primary} so this
     * configuration's by-type injections of the suite still resolve when an
     * application registers another {@link DatabaseAuditSuite} for a second
     * datasource.
     *
     * @param dataSource
     *                                the primary datasource to audit.
     * @param entityManagerFactory
     *                                the primary entity-manager factory whose
     *                                mappings the JPA audit validates against the
     *                                schema.
     * @param sqlCapturer
     *                                the shared SQL capturer the runtime audits
     *                                read.
     * @return the wired audit suite.
     */
    @Bean
    @Primary
    DatabaseAuditSuite databaseAuditSuite(final DataSource dataSource,
            final EntityManagerFactory entityManagerFactory,
            final SqlCapturingStatementInspector sqlCapturer) {
        return new DatabaseAuditSuite(dataSource, entityManagerFactory,
                sqlCapturer);
    }

    /**
     * Registers every {@code *AuditAssertion} the suite wires — and the
     * {@link DatabaseAuditAssertions} facade — as a bean under its concrete type,
     * replacing one hand-written {@code @Bean} method per audit. Registered as
     * {@code primary} with no name prefix, so these beans are the default an
     * unqualified by-type injection resolves to and keep the conventional bean
     * names an application may already reference. Consumers {@code @Autowired}
     * the assertion bean they need and call {@code assertClean(...)}.
     *
     * @param suite
     *                  the primary datasource's audit suite.
     * @return the registrar that publishes the suite's assertions as beans.
     */
    @Bean
    AuditAssertionRegistrar auditAssertionRegistrar(
            final DatabaseAuditSuite suite) {
        return new AuditAssertionRegistrar(suite, "", true);
    }
}
