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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.spring.boot.assertion.AuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;
import io.github.databaseaudits.spring.boot.assertion.JoinIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.OrderByIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.UnconditionalMutationAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.WhereClauseIndexAuditAssertion;
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
     * The suite bean constructs the whole graph — proving the wiring compiles
     * and runs against core's audit constructors and that the platform is
     * detected from the {@link DataSource}.
     */
    @Test
    void testDatabaseAuditSuiteBean_ConstructsAgainstCoreAuditConstructors()
            throws SQLException {
        final SqlCapturingStatementInspector capturer =
                configuration.sqlCapturingStatementInspector();

        final DatabaseAuditSuite suite = configuration.databaseAuditSuite(
                postgresDataSource(), mock(EntityManagerFactory.class),
                capturer);

        assertThat(suite.all())
                .as("The suite bean wires every audit assertion against core's constructors.")
                .isNotEmpty();
    }

    /**
     * The stock registrar publishes every one of the suite's assertions — and
     * the {@link DatabaseAuditAssertions} facade — as a primary bean under its
     * concrete type, so consumers {@code @Autowired} the assertion they need
     * without a per-audit {@code @Bean} method.
     */
    @Test
    void testAuditAssertionRegistrar_PublishesEveryAssertionAndTheFacadeAsPrimaryBeans()
            throws SQLException {
        final DatabaseAuditSuite suite = new DatabaseAuditSuite(
                postgresDataSource(), mock(EntityManagerFactory.class),
                new SqlCapturingStatementInspector());
        final DefaultListableBeanFactory beanFactory =
                new DefaultListableBeanFactory();
        final AuditAssertionRegistrar registrar =
                configuration.auditAssertionRegistrar(suite);
        registrar.setBeanFactory(beanFactory);

        registrar.afterSingletonsInstantiated();

        for (final AuditAssertion assertion : suite.all()) {
            assertThat(beanFactory.getBean(assertion.getClass()))
                    .as("The %s bean is the suite's own assertion instance.",
                            assertion.getClass().getSimpleName())
                    .isSameAs(assertion);
        }
        assertThat(beanFactory.getBean(DatabaseAuditAssertions.class))
                .as("The facade bean is the suite's assertions facade.")
                .isSameAs(suite.assertions());
        assertThat(beanFactory
                .getBeanDefinition("foreignKeyIndexAuditAssertion").isPrimary())
                .as("The stock registrar marks its assertion beans primary.")
                .isTrue();
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

    /**
     * The plan-based runtime assertions (Join/OrderBy/WhereClause) are
     * PostgreSQL-only, so a suite over a non-PostgreSQL datasource omits them
     * while keeping the capture-based mutation assertion — the facade's
     * runtime and all-family runs then stay clean on MySQL/MariaDB instead of
     * failing fast on a PostgreSQL-only audit.
     */
    @Test
    void testSuiteAll_NonPostgresPlatform_OmitsThePlanBasedRuntimeAssertions()
            throws SQLException {
        final DatabaseAuditSuite suite = new DatabaseAuditSuite(
                dataSourceReporting("MySQL"), mock(EntityManagerFactory.class),
                new SqlCapturingStatementInspector());

        assertThat(suite.all())
                .as("MySQL wires the mutation assertion but not the PostgreSQL-only plan assertions.")
                .noneMatch(assertion -> assertion instanceof JoinIndexAuditAssertion
                        || assertion instanceof OrderByIndexAuditAssertion
                        || assertion instanceof WhereClauseIndexAuditAssertion)
                .anyMatch(assertion -> assertion instanceof UnconditionalMutationAuditAssertion);
    }

    private static String simpleName(final String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static String auditName(final AuditAssertion assertion) {
        final String name = assertion.getClass().getSimpleName();
        return name.substring(0, name.length() - "Assertion".length());
    }

    private static DataSource postgresDataSource() throws SQLException {
        return dataSourceReporting("PostgreSQL");
    }

    private static DataSource dataSourceReporting(final String productName)
            throws SQLException {
        final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
        final Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metaData);
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}
