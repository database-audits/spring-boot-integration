package io.github.databaseaudits.spring.boot.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import io.github.databaseaudits.audit.finding.ForeignKeyIndexFinding;
import io.github.databaseaudits.audit.finding.UnconditionalMutationFinding;
import io.github.databaseaudits.platform.DatabasePlatform;
import io.github.databaseaudits.spring.boot.assertion.AuditFamily;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditFailure;

/**
 * Unit tests for {@link DatabaseAuditReport}: findings group by family and
 * audit, the fix section appears only when requested, and each
 * {@link ReportFormat} applies its own markup.
 */
class DatabaseAuditReportTest {
    private static final DatabaseAuditFailure CATALOG_FAILURE =
            new DatabaseAuditFailure(AuditFamily.CATALOG, "ForeignKeyIndexAudit",
                    "Foreign keys with no supporting index.",
                    List.of(new ForeignKeyIndexFinding("child",
                            "fk_child_parent", List.of("parent_id"), "parent")),
                    DatabasePlatform.POSTGRESQL);

    private static final DatabaseAuditFailure RUNTIME_FAILURE =
            new DatabaseAuditFailure(AuditFamily.RUNTIME,
                    "UnconditionalMutationAudit", "Full-table mutation.",
                    List.of(new UnconditionalMutationFinding(
                            "delete from orders")),
                    DatabasePlatform.POSTGRESQL);

    private final FixRenderer fixRenderer = new FixRenderer();

    @Test
    void testRender_MarkdownNoFixes_GroupsByFamilyAndAuditWithoutFixSection() {
        final String report = DatabaseAuditReport.render(
                List.of(RUNTIME_FAILURE, CATALOG_FAILURE), ReportFormat.MARKDOWN,
                FixFormat.NONE, FixPlacement.BOTH, fixRenderer);

        assertThat(report)
                .as("The report titles, summarizes, and groups findings by family and audit.")
                .contains("# Database Audit Report")
                .contains("2 findings across 2 audits.").contains("## CATALOG")
                .contains("### ForeignKeyIndexAudit")
                .contains("Foreign keys with no supporting index.")
                .contains(
                        "child.fk_child_parent  ->  FOREIGN KEY (parent_id) REFERENCES parent")
                .contains("## RUNTIME").contains("delete from orders");
        assertThat(report.indexOf("## CATALOG"))
                .as("Families render in enum order — CATALOG before RUNTIME.")
                .isLessThan(report.indexOf("## RUNTIME"));
        assertThat(report).as("No fixes are emitted when the fix format is none.")
                .doesNotContain("Suggested fixes");
    }

    @Test
    void testRender_MarkdownSqlFixes_AppendsLabeledSqlBlock() {
        final String report = DatabaseAuditReport.render(
                List.of(CATALOG_FAILURE), ReportFormat.MARKDOWN, FixFormat.SQL,
                FixPlacement.SECTION, fixRenderer);

        assertThat(report)
                .as("SQL fixes append a fenced sql block labeling each fix's audit and fidelity.")
                .contains("## Suggested fixes (sql)").contains("```sql")
                .contains("-- ForeignKeyIndexAudit [precise]").contains(
                        "CREATE INDEX ix_child_parent_id ON child (parent_id);");
    }

    @Test
    void testRender_AsciiDocLiquibase_EmbedsAWellFormedChangelog() {
        final String report = DatabaseAuditReport.render(
                List.of(CATALOG_FAILURE), ReportFormat.ASCIIDOC,
                FixFormat.LIQUIBASE_XML, FixPlacement.SECTION, fixRenderer);

        assertThat(report)
                .as("AsciiDoc uses = headings and a source block holding one databaseChangeLog.")
                .contains("= Database Audit Report").contains("== CATALOG")
                .contains("[source,xml]")
                .contains("<databaseChangeLog").contains("<changeSet id=")
                .contains("</databaseChangeLog>");
    }

    @Test
    void testRender_LiquibaseSection_DeduplicatesIdenticalChangeSets() {
        final String report = DatabaseAuditReport.render(
                List.of(CATALOG_FAILURE, CATALOG_FAILURE), ReportFormat.MARKDOWN,
                FixFormat.LIQUIBASE_XML, FixPlacement.SECTION, fixRenderer);

        final long changeSetCount =
                report.lines().filter(line -> line.contains("<changeSet id=\""))
                        .count();
        assertThat(changeSetCount)
                .as("Two failures carrying the identical finding collapse to one "
                        + "change set, so the standalone changelog has no duplicate id.")
                .isEqualTo(1);
    }

    @Test
    void testRender_Liquibase_EmbeddedChangelogIsWellFormedXml()
            throws Exception {
        final String report = DatabaseAuditReport.render(
                List.of(CATALOG_FAILURE, RUNTIME_FAILURE), ReportFormat.MARKDOWN,
                FixFormat.LIQUIBASE_XML, FixPlacement.SECTION, fixRenderer);

        final String xml = report.substring(report.indexOf("<?xml"),
                report.indexOf("</databaseChangeLog>")
                        + "</databaseChangeLog>".length());
        final Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        assertThat(document.getDocumentElement().getNodeName())
                .as("The embedded changelog (a precise change set plus an advisory comment) is well-formed XML.")
                .isEqualTo("databaseChangeLog");
    }

    @Test
    void testRender_TextFormat_UnderlinesHeadings() {
        final String report = DatabaseAuditReport.render(List.of(CATALOG_FAILURE),
                ReportFormat.TEXT, FixFormat.NONE, FixPlacement.BOTH,
                fixRenderer);

        assertThat(report)
                .as("Text format underlines the title and family headings.")
                .contains("Database Audit Report\n=====================")
                .contains("CATALOG\n-------");
    }

    @Test
    void testRender_InlinePlacement_PutsFixUnderEachAuditWithoutSection() {
        final String report = DatabaseAuditReport.render(List.of(CATALOG_FAILURE),
                ReportFormat.MARKDOWN, FixFormat.SQL, FixPlacement.INLINE,
                fixRenderer);

        assertThat(report)
                .as("Inline placement renders each audit's fix under its findings and omits the consolidated section.")
                .contains("### ForeignKeyIndexAudit")
                .contains("Suggested fixes (sql):").contains(
                        "CREATE INDEX ix_child_parent_id ON child (parent_id);")
                .doesNotContain("## Suggested fixes");
        assertThat(report.indexOf("### ForeignKeyIndexAudit"))
                .as("The inline fix follows the audit's heading.")
                .isLessThan(report.indexOf("Suggested fixes (sql):"));
    }

    @Test
    void testRender_BothPlacement_ShowsFixInlineAndInSection() {
        final String report = DatabaseAuditReport.render(List.of(CATALOG_FAILURE),
                ReportFormat.MARKDOWN, FixFormat.SQL, FixPlacement.BOTH,
                fixRenderer);

        assertThat(report)
                .as("Both placement renders the fix inline and in the consolidated section.")
                .contains("Suggested fixes (sql):")
                .contains("## Suggested fixes (sql)");
        assertThat(report.indexOf("CREATE INDEX"))
                .as("The fix appears twice — inline and in the consolidated section.")
                .isNotEqualTo(report.lastIndexOf("CREATE INDEX"));
    }
}
