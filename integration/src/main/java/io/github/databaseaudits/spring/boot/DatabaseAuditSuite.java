package io.github.databaseaudits.spring.boot;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import io.github.databaseaudits.audit.catalog.DuplicateForeignKeyAudit;
import io.github.databaseaudits.audit.catalog.ForeignKeyIndexAudit;
import io.github.databaseaudits.audit.catalog.ForeignKeyNotNullAudit;
import io.github.databaseaudits.audit.catalog.ForeignKeyTypeMatchAudit;
import io.github.databaseaudits.audit.catalog.PrimaryKeyPresenceAudit;
import io.github.databaseaudits.audit.catalog.PrimaryKeyTypeAudit;
import io.github.databaseaudits.audit.catalog.RedundantIndexAudit;
import io.github.databaseaudits.audit.catalog.UniqueIndexNotNullAudit;
import io.github.databaseaudits.audit.jpa.EagerCollectionFetchAudit;
import io.github.databaseaudits.audit.jpa.MissingVersionAttributeAudit;
import io.github.databaseaudits.audit.jpa.SchemaEntityValidationAudit;
import io.github.databaseaudits.audit.jpa.UnmappedDatabaseObjectAudit;
import io.github.databaseaudits.audit.runtime.OffsetPaginationAudit;
import io.github.databaseaudits.audit.runtime.RepeatedStatementAudit;
import io.github.databaseaudits.audit.runtime.UnconditionalMutationAudit;
import io.github.databaseaudits.audit.runtime.plan.JoinIndexAudit;
import io.github.databaseaudits.audit.runtime.plan.OrderByIndexAudit;
import io.github.databaseaudits.audit.runtime.plan.UnusedIndexAudit;
import io.github.databaseaudits.audit.runtime.plan.WhereClauseIndexAudit;
import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import io.github.databaseaudits.catalog.ForeignKeyCatalog;
import io.github.databaseaudits.catalog.IndexCatalog;
import io.github.databaseaudits.jdbc.CatalogQueries;
import io.github.databaseaudits.plan.QueryPlanExplainer;
import io.github.databaseaudits.platform.DatabasePlatform;
import io.github.databaseaudits.spring.boot.assertion.AuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;
import io.github.databaseaudits.spring.boot.assertion.DuplicateForeignKeyAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.EagerCollectionFetchAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyNotNullAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyTypeMatchAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.JoinIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.MissingVersionAttributeAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.OffsetPaginationAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.OrderByIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.PrimaryKeyPresenceAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.PrimaryKeyTypeAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.RedundantIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.RepeatedStatementAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.SchemaEntityValidationAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.UnconditionalMutationAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.UniqueIndexNotNullAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.UnmappedDatabaseObjectAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.UnusedIndexAuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.WhereClauseIndexAuditAssertion;
import jakarta.persistence.EntityManagerFactory;

/**
 * Wires the whole database-audit suite for one
 * {@code (DataSource, EntityManagerFactory)} pair. This is the single place that
 * mirrors core's audit constructors: it detects the platform, builds the catalog
 * collaborators ({@link CatalogQueries}, {@link IndexCatalog}), the
 * {@link QueryPlanExplainer}, every audit, and the paired {@code *AuditAssertion}
 * — collected into {@link #all()} and surfaced through the
 * {@link DatabaseAuditAssertions} facade. Adding a core audit means adding one
 * {@code new XxxAuditAssertion(new XxxAudit(...))} line here; nothing else
 * enumerates the roster.
 *
 * <p>
 * The plan-based runtime audits ({@code JoinIndexAudit}, {@code OrderByIndexAudit},
 * {@code WhereClauseIndexAudit}) are PostgreSQL-only, so {@link #all()} wires them
 * only when the detected platform is PostgreSQL. On MySQL/MariaDB they are absent
 * from the roster, so the facade's {@code assertRuntimeClean}/{@code assertAllClean}
 * run only the audits the platform supports rather than failing fast on a
 * PostgreSQL-only audit.
 *
 * <p>
 * {@link DatabaseAuditTestConfiguration} builds one of these from the
 * context's primary {@link DataSource}/{@link EntityManagerFactory} and, through
 * an {@link AuditAssertionRegistrar} over {@link #all()}, registers its assertions
 * as beans. An application with several datasources adds one small
 * {@code @TestConfiguration} per extra datasource that constructs another suite
 * from its qualified beans and registers {@link #all()} the same way — see the
 * integration usage docs.
 *
 * <p>
 * The caller supplies the {@link SqlCapturingStatementInspector}, because the
 * runtime audits read SQL from the <em>same instance</em> that must also be
 * registered as that {@link EntityManagerFactory}'s Hibernate
 * {@code StatementInspector}. Constructing a suite never opens an
 * {@code EXPLAIN}; the plan-based audits are wired only on PostgreSQL (the only
 * platform that can run them).
 */
public class DatabaseAuditSuite {
    private final List<AuditAssertion> all;

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
        final ForeignKeyCatalog foreignKeyCatalog =
                new ForeignKeyCatalog(catalogQueries, platform);

        // The one legitimate enumeration of the roster, in family order
        // (catalog, JPA, runtime); everything else drives it through all().
        // The explicit type witness keeps javac's common-supertype inference
        // from picking the package-private AbstractAuditAssertion (compiling
        // an inaccessible-type error) instead of the public AuditAssertion.
        final List<AuditAssertion> roster = new ArrayList<>(List.<AuditAssertion>of(
                new DuplicateForeignKeyAuditAssertion(
                        new DuplicateForeignKeyAudit(foreignKeyCatalog),
                        platform),
                new ForeignKeyIndexAuditAssertion(new ForeignKeyIndexAudit(
                        catalogQueries, indexCatalog, platform), platform),
                new ForeignKeyNotNullAuditAssertion(
                        new ForeignKeyNotNullAudit(catalogQueries, platform),
                        platform),
                new ForeignKeyTypeMatchAuditAssertion(
                        new ForeignKeyTypeMatchAudit(catalogQueries, platform),
                        platform),
                new PrimaryKeyPresenceAuditAssertion(
                        new PrimaryKeyPresenceAudit(catalogQueries, platform),
                        platform),
                new PrimaryKeyTypeAuditAssertion(
                        new PrimaryKeyTypeAudit(catalogQueries, platform),
                        platform),
                new RedundantIndexAuditAssertion(
                        new RedundantIndexAudit(indexCatalog), platform),
                new UniqueIndexNotNullAuditAssertion(
                        new UniqueIndexNotNullAudit(catalogQueries,
                                indexCatalog, platform),
                        platform),
                new EagerCollectionFetchAuditAssertion(
                        EagerCollectionFetchAudit.forEntityManagerFactory(
                                entityManagerFactory),
                        platform),
                new MissingVersionAttributeAuditAssertion(
                        MissingVersionAttributeAudit.forEntityManagerFactory(
                                entityManagerFactory),
                        platform),
                new SchemaEntityValidationAuditAssertion(
                        SchemaEntityValidationAudit.forEntityManagerFactory(
                                entityManagerFactory, dataSource),
                        platform),
                new UnmappedDatabaseObjectAuditAssertion(
                        UnmappedDatabaseObjectAudit.forEntityManagerFactory(
                                entityManagerFactory, dataSource),
                        platform)));

        // The plan-based runtime audits (Join/OrderBy/Unused/WhereClause) are
        // PostgreSQL-only — they fail fast on every other platform — so they are
        // wired only where the platform can run them; the facade's runtime and
        // all-family runs then stay clean on MySQL/MariaDB. The capture-based
        // OffsetPagination/RepeatedStatement/UnconditionalMutation audits run on
        // every platform.
        if (platform == DatabasePlatform.POSTGRESQL) {
            final QueryPlanExplainer queryPlanExplainer =
                    new QueryPlanExplainer(dataSource, platform);
            roster.add(new JoinIndexAuditAssertion(
                    new JoinIndexAudit(queryPlanExplainer, sqlCapturer),
                    platform));
            roster.add(new OrderByIndexAuditAssertion(
                    new OrderByIndexAudit(queryPlanExplainer, sqlCapturer),
                    platform));
            roster.add(new UnusedIndexAuditAssertion(
                    new UnusedIndexAudit(queryPlanExplainer, sqlCapturer,
                            indexCatalog, foreignKeyCatalog),
                    platform));
            roster.add(new WhereClauseIndexAuditAssertion(
                    new WhereClauseIndexAudit(queryPlanExplainer, sqlCapturer),
                    platform));
        }
        roster.add(new OffsetPaginationAuditAssertion(
                new OffsetPaginationAudit(sqlCapturer), platform));
        roster.add(new RepeatedStatementAuditAssertion(
                new RepeatedStatementAudit(sqlCapturer), platform));
        roster.add(new UnconditionalMutationAuditAssertion(
                new UnconditionalMutationAudit(sqlCapturer), platform));

        this.all = List.copyOf(roster);
        this.assertions = new DatabaseAuditAssertions(all);
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
     * Returns every audit assertion this suite wires, in family order (catalog,
     * JPA, runtime). Backs the {@link DatabaseAuditAssertions} facade and the
     * {@link AuditAssertionRegistrar} bean registration, and lets a roster guard
     * verify every core audit has a wired assertion. On PostgreSQL this is the
     * whole roster; on other platforms it omits the PostgreSQL-only plan-based
     * runtime assertions.
     *
     * @return the wired audit assertions.
     */
    public List<AuditAssertion> all() {
        return all;
    }
}
