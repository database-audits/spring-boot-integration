package io.github.databaseaudits.spring.boot.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.databaseaudits.audit.finding.ForeignKeyIndexFinding;
import io.github.databaseaudits.audit.finding.ForeignKeyNotNullFinding;
import io.github.databaseaudits.audit.finding.ForeignKeyTypeMismatchFinding;
import io.github.databaseaudits.audit.finding.MissingPrimaryKeyFinding;
import io.github.databaseaudits.audit.finding.PlanIndexFinding;
import io.github.databaseaudits.audit.finding.RedundantIndexFinding;
import io.github.databaseaudits.audit.finding.SchemaColumnMissingFinding;
import io.github.databaseaudits.audit.finding.SchemaColumnTypeMismatchFinding;
import io.github.databaseaudits.audit.finding.SchemaTableMissingFinding;
import io.github.databaseaudits.audit.finding.UnconditionalMutationFinding;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Unit tests for {@link FixRenderer}: each finding kind renders the expected
 * DDL, the dialect-divergent fixes branch on platform, and Liquibase change-set
 * wrapping is precise-only and stable.
 */
class FixRendererTest {
    private final FixRenderer renderer = new FixRenderer();

    @Test
    void testFixFor_UnindexedForeignKey_RendersPreciseCreateIndex() {
        final ForeignKeyIndexFinding finding = new ForeignKeyIndexFinding(
                "child", "fk_child_parent", List.of("a", "b"), "parent");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("Unindexed FK renders a precise composite CREATE INDEX.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "CREATE INDEX ix_child_a_b ON child (a, b);"));
    }

    @Test
    void testFixFor_RedundantIndex_DropSyntaxDiffersByPlatform() {
        final RedundantIndexFinding finding = new RedundantIndexFinding("orders",
                "idx_customer", "idx_customer_created");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("PostgreSQL drops an index by name alone.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "DROP INDEX idx_customer;"));
        assertThat(renderer.fixFor(finding, DatabasePlatform.MYSQL))
                .as("MySQL requires the owning table in DROP INDEX.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "DROP INDEX idx_customer ON orders;"));
    }

    @Test
    void testFixFor_NullableForeignKey_PreciseOnPostgresTemplateOnMysql() {
        final ForeignKeyNotNullFinding finding = new ForeignKeyNotNullFinding(
                "child", "parent_id", "fk_child_parent");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("PostgreSQL SET NOT NULL is precise.")
                .satisfies(fix -> {
                    assertThat(fix.fidelity()).as("The fix is precise.")
                            .isEqualTo(FixFidelity.PRECISE);
                    assertThat(fix.ddl()).as("The DDL sets the column not null.")
                            .contains(
                                    "ALTER TABLE child ALTER COLUMN parent_id SET NOT NULL;");
                });
        assertThat(renderer.fixFor(finding, DatabasePlatform.MARIADB))
                .as("The MySQL family needs the column type, so the fix is a template.")
                .satisfies(fix -> {
                    assertThat(fix.fidelity()).as("The fix is a template.")
                            .isEqualTo(FixFidelity.TEMPLATE);
                    assertThat(fix.ddl())
                            .as("The DDL is a MODIFY with a TODO column type.")
                            .contains("MODIFY parent_id").contains("NOT NULL")
                            .contains("TODO");
                });
    }

    @Test
    void testFixFor_ForeignKeyTypeMismatch_AlignsToReferencedTypeByDialect() {
        final ForeignKeyTypeMismatchFinding finding =
                new ForeignKeyTypeMismatchFinding("child", "parent_id",
                        "integer", "parent", "id", "bigint", "fk_child_parent");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("PostgreSQL alters the column type to the referenced type.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "ALTER TABLE child ALTER COLUMN parent_id TYPE bigint;"));
        assertThat(renderer.fixFor(finding, DatabasePlatform.MYSQL))
                .as("MySQL uses MODIFY to change the column type.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "ALTER TABLE child MODIFY parent_id bigint;"));
    }

    @Test
    void testFixFor_MissingColumn_RendersPreciseAddColumn() {
        final SchemaColumnMissingFinding finding =
                new SchemaColumnMissingFinding("parent", "quantity", "integer");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("A missing mapped column renders a precise ADD COLUMN.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "ALTER TABLE parent ADD COLUMN quantity integer;"));
    }

    @Test
    void testFixFor_ColumnTypeMismatch_AltersToExpectedType() {
        final SchemaColumnTypeMismatchFinding finding =
                new SchemaColumnTypeMismatchFinding("parent", "name", "integer",
                        "varchar(255)");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("A mapped-column type mismatch alters to the expected type.")
                .isEqualTo(new Fix(FixFidelity.PRECISE,
                        "ALTER TABLE parent ALTER COLUMN name TYPE varchar(255);"));
    }

    @Test
    void testFixFor_MissingTable_IsATemplateCreateTable() {
        assertThat(renderer.fixFor(new SchemaTableMissingFinding("child"),
                DatabasePlatform.POSTGRESQL))
                .as("A missing table cannot be fully specified, so it is a template.")
                .satisfies(fix -> {
                    assertThat(fix.fidelity()).as("The fix is a template.")
                            .isEqualTo(FixFidelity.TEMPLATE);
                    assertThat(fix.ddl())
                            .as("The DDL is a CREATE TABLE with a TODO for columns.")
                            .contains("CREATE TABLE child").contains("TODO");
                });
    }

    @Test
    void testFixFor_MissingPrimaryKey_IsATemplateWithTodoColumns() {
        assertThat(renderer.fixFor(new MissingPrimaryKeyFinding("orders"),
                DatabasePlatform.POSTGRESQL))
                .as("A missing PK's columns are unknown, so the fix is a template.")
                .isEqualTo(new Fix(FixFidelity.TEMPLATE,
                        "ALTER TABLE orders ADD PRIMARY KEY (/* TODO: key column(s) */);"));
    }

    @Test
    void testFixFor_PlanFinding_IsBestEffortWithRelationParsedFromPlan() {
        final PlanIndexFinding finding = new PlanIndexFinding(
                "select * from orders where id = ?",
                "Seq Scan on 'orders' filtering (id = $1)");

        assertThat(renderer.fixFor(finding, DatabasePlatform.POSTGRESQL))
                .as("A plan finding yields a best-effort CREATE INDEX on the parsed relation.")
                .satisfies(fix -> {
                    assertThat(fix.fidelity()).as("The fix is best-effort.")
                            .isEqualTo(FixFidelity.BEST_EFFORT);
                    assertThat(fix.ddl())
                            .as("The DDL suggests an index on the parsed table, "
                                    + "carrying the plan and statement for review.")
                            .contains("CREATE INDEX").contains("ON orders")
                            .contains("TODO")
                            .contains("select * from orders where id = ?");
                });
    }

    @Test
    void testFixFor_UnconditionalMutation_IsAdvisoryOnly() {
        assertThat(renderer.fixFor(
                new UnconditionalMutationFinding("delete from orders"),
                DatabasePlatform.POSTGRESQL))
                .as("A full-table mutation has no DDL fix, only WHERE-clause advice.")
                .satisfies(fix -> {
                    assertThat(fix.fidelity()).as("The fix is advisory only.")
                            .isEqualTo(FixFidelity.ADVISORY);
                    assertThat(fix.ddl())
                            .as("The advice quotes the offending statement.")
                            .contains("WHERE").contains("delete from orders");
                });
    }

    @Test
    void testLiquibaseChangeSet_UnindexedForeignKey_IsACreateIndexChangeSet() {
        final ForeignKeyIndexFinding finding = new ForeignKeyIndexFinding(
                "child", "fk_child_parent", List.of("parent_id"), "parent");

        final String changeSet = renderer.liquibaseChangeSet(finding,
                DatabasePlatform.POSTGRESQL);

        assertThat(changeSet)
                .as("A precise fix is a runnable change set built from a native change type, with a stable id.")
                .contains("<changeSet id=\"database-audits-")
                .contains("author=\"database-audits\"")
                .contains(
                        "<createIndex indexName=\"ix_child_parent_id\" tableName=\"child\">")
                .contains("<column name=\"parent_id\"/>")
                .contains("</createIndex>").doesNotContain("<sql>");
        assertThat(renderer.liquibaseChangeSet(finding,
                DatabasePlatform.POSTGRESQL))
                .as("The same finding yields the same change set on a re-run.")
                .isEqualTo(changeSet);
    }

    @Test
    void testLiquibaseChangeSet_RedundantIndex_IsADropIndexChangeSet() {
        assertThat(renderer.liquibaseChangeSet(
                new RedundantIndexFinding("orders", "idx_customer",
                        "idx_customer_created"),
                DatabasePlatform.MYSQL))
                .as("A redundant index drops via the native dropIndex type — Liquibase renders the dialect SQL.")
                .contains(
                        "<dropIndex indexName=\"idx_customer\" tableName=\"orders\"/>")
                .doesNotContain("<sql>");
    }

    @Test
    void testLiquibaseChangeSet_ForeignKeyTypeMismatch_IsAModifyDataTypeChangeSet() {
        assertThat(renderer.liquibaseChangeSet(
                new ForeignKeyTypeMismatchFinding("child", "parent_id",
                        "integer", "parent", "id", "bigint", "fk_child_parent"),
                DatabasePlatform.POSTGRESQL))
                .as("A foreign-key type mismatch aligns the column via modifyDataType.")
                .contains(
                        "<modifyDataType tableName=\"child\" columnName=\"parent_id\" newDataType=\"bigint\"/>");
    }

    @Test
    void testLiquibaseChangeSet_MissingColumn_IsAnAddColumnSplittingSchema() {
        assertThat(renderer.liquibaseChangeSet(
                new SchemaColumnMissingFinding("public.parent", "quantity",
                        "integer"),
                DatabasePlatform.POSTGRESQL))
                .as("A missing mapped column adds via addColumn, splitting the schema-qualified table.")
                .contains("<addColumn schemaName=\"public\" tableName=\"parent\">")
                .contains("<column name=\"quantity\" type=\"integer\"/>")
                .contains("</addColumn>");
    }

    @Test
    void testLiquibaseChangeSet_NullableForeignKey_PostgresIsAddNotNullConstraint() {
        assertThat(renderer.liquibaseChangeSet(
                new ForeignKeyNotNullFinding("child", "parent_id",
                        "fk_child_parent"),
                DatabasePlatform.POSTGRESQL))
                .as("On PostgreSQL a nullable foreign key becomes a runnable "
                        + "addNotNullConstraint preceded by the backfill warning "
                        + "the SQL fix also carries.")
                .contains(
                        "<addNotNullConstraint tableName=\"child\" columnName=\"parent_id\"/>")
                .contains(
                        "NOTE: fails if the column currently holds NULLs");
    }

    @Test
    void testLiquibaseChangeSet_NullableForeignKey_MysqlIsAComment() {
        assertThat(renderer.liquibaseChangeSet(
                new ForeignKeyNotNullFinding("child", "parent_id",
                        "fk_child_parent"),
                DatabasePlatform.MYSQL))
                .as("On MySQL the column type is unknown, so the not-null fix degrades to a comment, not a broken change.")
                .startsWith("    <!--").doesNotContain("<addNotNullConstraint");
    }

    @Test
    void testLiquibaseChangeSet_MissingTable_IsACommentNotAChangeSet() {
        assertThat(renderer.liquibaseChangeSet(
                new SchemaTableMissingFinding("child"),
                DatabasePlatform.POSTGRESQL))
                .as("A missing table's columns are unknown, so it degrades to a comment, not a broken change.")
                .startsWith("    <!--").doesNotContain("<changeSet");
    }

    @Test
    void testLiquibaseChangeSet_MissingPrimaryKey_IsACommentNotAChangeSet() {
        assertThat(renderer.liquibaseChangeSet(
                new MissingPrimaryKeyFinding("orders"), DatabasePlatform.POSTGRESQL))
                .as("A missing PK's key column(s) are unknown, so it degrades to a comment, not a broken change.")
                .startsWith("    <!--").doesNotContain("<changeSet");
    }

    @Test
    void testLiquibaseChangeSet_PlanFinding_IsACommentNotAChangeSet() {
        assertThat(renderer.liquibaseChangeSet(
                new PlanIndexFinding("select * from orders where id = ?",
                        "Seq Scan on 'orders' filtering (id = $1)"),
                DatabasePlatform.POSTGRESQL))
                .as("A plan-based suggestion is best-effort, so it degrades to a comment, not a broken change.")
                .startsWith("    <!--").doesNotContain("<changeSet");
    }

    @Test
    void testLiquibaseChangeSet_AdvisoryFinding_IsACommentNotAChangeSet() {
        final String comment = renderer.liquibaseChangeSet(
                new UnconditionalMutationFinding("delete from orders"),
                DatabasePlatform.POSTGRESQL);

        assertThat(comment)
                .as("A non-precise fix is emitted as a comment, not a change set.")
                .startsWith("    <!--").endsWith("-->")
                .doesNotContain("<changeSet");
        final String body =
                comment.substring(comment.indexOf("<!--") + "<!--".length(),
                        comment.length() - "-->".length());
        assertThat(body)
                .as("An XML comment body must not contain '--' (invalid XML).")
                .doesNotContain("--");
    }
}
