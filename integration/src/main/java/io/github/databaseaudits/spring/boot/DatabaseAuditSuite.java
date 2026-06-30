package io.github.databaseaudits.spring.boot;

import javax.sql.DataSource;

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
 * Wires the whole database-audit suite for one
 * {@code (DataSource, EntityManagerFactory)} pair. This is the single place that
 * mirrors core's audit constructors: it detects the platform, builds the catalog
 * collaborators ({@link CatalogQueries}, {@link IndexCatalog}), the
 * {@link QueryPlanExplainer}, every audit, and the paired {@code *AuditAssertion}
 * surfaced through the {@link DatabaseAuditAssertions} facade.
 *
 * <p>
 * {@link DatabaseAuditTestConfiguration} builds one of these from the
 * context's primary {@link DataSource}/{@link EntityManagerFactory} and registers
 * its assertions as beans. An application with several datasources adds one small
 * {@code @TestConfiguration} per extra datasource that constructs another suite
 * from its qualified beans and exposes {@link #assertions()} as a named bean —
 * see the integration usage docs.
 *
 * <p>
 * The caller supplies the {@link SqlCapturingStatementInspector}, because the
 * runtime audits read SQL from the <em>same instance</em> that must also be
 * registered as that {@link EntityManagerFactory}'s Hibernate
 * {@code StatementInspector}. Constructing a suite never opens an
 * {@code EXPLAIN}; the plan-based audits stay PostgreSQL-only and fail fast only
 * when run on another platform.
 */
public class DatabaseAuditSuite {
    private final ForeignKeyIndexAuditAssertion foreignKeyIndexAuditAssertion;
    private final ForeignKeyNotNullAuditAssertion foreignKeyNotNullAuditAssertion;
    private final ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatchAuditAssertion;
    private final PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion;
    private final RedundantIndexAuditAssertion redundantIndexAuditAssertion;
    private final SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion;
    private final JoinIndexAuditAssertion joinIndexAuditAssertion;
    private final OrderByIndexAuditAssertion orderByIndexAuditAssertion;
    private final WhereClauseIndexAuditAssertion whereClauseIndexAuditAssertion;
    private final UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion;
    private final DatabaseAuditAssertions assertions;

    /**
     * Builds the full audit graph for one datasource.
     *
     * @param dataSource
     *                                the datasource whose schema the catalog and
     *                                runtime audits inspect; its platform is
     *                                auto-detected from its metadata.
     * @param entityManagerFactory
     *                                the entity-manager factory whose entity
     *                                mappings the JPA audit validates against the
     *                                schema.
     * @param sqlCapturer
     *                                the SQL capturer the runtime audits read;
     *                                must be the same instance registered as the
     *                                {@code entityManagerFactory}'s Hibernate
     *                                {@code StatementInspector}.
     */
    public DatabaseAuditSuite(final DataSource dataSource,
            final EntityManagerFactory entityManagerFactory,
            final SqlCapturingStatementInspector sqlCapturer) {
        final DatabasePlatform platform =
                DatabasePlatform.fromDataSource(dataSource);
        final CatalogQueries catalogQueries = new CatalogQueries(dataSource);
        final IndexCatalog indexCatalog =
                new IndexCatalog(catalogQueries, platform);
        final QueryPlanExplainer queryPlanExplainer =
                new QueryPlanExplainer(dataSource, platform);

        this.foreignKeyIndexAuditAssertion =
                new ForeignKeyIndexAuditAssertion(new ForeignKeyIndexAudit(
                        catalogQueries, indexCatalog, platform));
        this.foreignKeyNotNullAuditAssertion =
                new ForeignKeyNotNullAuditAssertion(
                        new ForeignKeyNotNullAudit(catalogQueries, platform));
        this.foreignKeyTypeMatchAuditAssertion =
                new ForeignKeyTypeMatchAuditAssertion(
                        new ForeignKeyTypeMatchAudit(catalogQueries, platform));
        this.primaryKeyPresenceAuditAssertion =
                new PrimaryKeyPresenceAuditAssertion(
                        new PrimaryKeyPresenceAudit(catalogQueries, platform));
        this.redundantIndexAuditAssertion = new RedundantIndexAuditAssertion(
                new RedundantIndexAudit(indexCatalog));
        this.schemaEntityValidationAuditAssertion =
                new SchemaEntityValidationAuditAssertion(
                        SchemaEntityValidationAudit.forEntityManagerFactory(
                                entityManagerFactory, dataSource));
        this.joinIndexAuditAssertion = new JoinIndexAuditAssertion(
                new JoinIndexAudit(queryPlanExplainer, sqlCapturer));
        this.orderByIndexAuditAssertion = new OrderByIndexAuditAssertion(
                new OrderByIndexAudit(queryPlanExplainer, sqlCapturer));
        this.whereClauseIndexAuditAssertion =
                new WhereClauseIndexAuditAssertion(
                        new WhereClauseIndexAudit(queryPlanExplainer,
                                sqlCapturer));
        this.unconditionalMutationAuditAssertion =
                new UnconditionalMutationAuditAssertion(
                        new UnconditionalMutationAudit(sqlCapturer));
        this.assertions = new DatabaseAuditAssertions(
                foreignKeyIndexAuditAssertion, foreignKeyNotNullAuditAssertion,
                foreignKeyTypeMatchAuditAssertion,
                primaryKeyPresenceAuditAssertion, redundantIndexAuditAssertion,
                schemaEntityValidationAuditAssertion, joinIndexAuditAssertion,
                orderByIndexAuditAssertion, whereClauseIndexAuditAssertion,
                unconditionalMutationAuditAssertion);
    }

    /**
     * Returns the facade that runs whole audit families in one call.
     *
     * @return the database-audit assertions facade.
     */
    public DatabaseAuditAssertions assertions() {
        return assertions;
    }

    /**
     * Returns the foreign-key index assertion.
     *
     * @return the foreign-key index assertion.
     */
    public ForeignKeyIndexAuditAssertion foreignKeyIndexAuditAssertion() {
        return foreignKeyIndexAuditAssertion;
    }

    /**
     * Returns the foreign-key not-null assertion.
     *
     * @return the foreign-key not-null assertion.
     */
    public ForeignKeyNotNullAuditAssertion foreignKeyNotNullAuditAssertion() {
        return foreignKeyNotNullAuditAssertion;
    }

    /**
     * Returns the foreign-key type-match assertion.
     *
     * @return the foreign-key type-match assertion.
     */
    public ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatchAuditAssertion() {
        return foreignKeyTypeMatchAuditAssertion;
    }

    /**
     * Returns the primary-key presence assertion.
     *
     * @return the primary-key presence assertion.
     */
    public PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion() {
        return primaryKeyPresenceAuditAssertion;
    }

    /**
     * Returns the redundant-index assertion.
     *
     * @return the redundant-index assertion.
     */
    public RedundantIndexAuditAssertion redundantIndexAuditAssertion() {
        return redundantIndexAuditAssertion;
    }

    /**
     * Returns the JPA schema/entity validation assertion.
     *
     * @return the JPA schema/entity validation assertion.
     */
    public SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion() {
        return schemaEntityValidationAuditAssertion;
    }

    /**
     * Returns the join-index assertion.
     *
     * @return the join-index assertion.
     */
    public JoinIndexAuditAssertion joinIndexAuditAssertion() {
        return joinIndexAuditAssertion;
    }

    /**
     * Returns the order-by index assertion.
     *
     * @return the order-by index assertion.
     */
    public OrderByIndexAuditAssertion orderByIndexAuditAssertion() {
        return orderByIndexAuditAssertion;
    }

    /**
     * Returns the where-clause index assertion.
     *
     * @return the where-clause index assertion.
     */
    public WhereClauseIndexAuditAssertion whereClauseIndexAuditAssertion() {
        return whereClauseIndexAuditAssertion;
    }

    /**
     * Returns the unconditional-mutation assertion.
     *
     * @return the unconditional-mutation assertion.
     */
    public UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion() {
        return unconditionalMutationAuditAssertion;
    }
}
