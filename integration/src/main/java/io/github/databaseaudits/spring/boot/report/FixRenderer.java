package io.github.databaseaudits.spring.boot.report;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.databaseaudits.audit.finding.DuplicateForeignKeyFinding;
import io.github.databaseaudits.audit.finding.EagerCollectionFetchFinding;
import io.github.databaseaudits.audit.finding.Finding;
import io.github.databaseaudits.audit.finding.ForeignKeyIndexFinding;
import io.github.databaseaudits.audit.finding.ForeignKeyNotNullFinding;
import io.github.databaseaudits.audit.finding.ForeignKeyTypeMismatchFinding;
import io.github.databaseaudits.audit.finding.MissingPrimaryKeyFinding;
import io.github.databaseaudits.audit.finding.MissingVersionAttributeFinding;
import io.github.databaseaudits.audit.finding.NarrowPrimaryKeyFinding;
import io.github.databaseaudits.audit.finding.OffsetPaginationFinding;
import io.github.databaseaudits.audit.finding.PlanIndexFinding;
import io.github.databaseaudits.audit.finding.RedundantIndexFinding;
import io.github.databaseaudits.audit.finding.RepeatedStatementFinding;
import io.github.databaseaudits.audit.finding.SchemaColumnMissingFinding;
import io.github.databaseaudits.audit.finding.SchemaColumnTypeMismatchFinding;
import io.github.databaseaudits.audit.finding.SchemaTableMissingFinding;
import io.github.databaseaudits.audit.finding.UnconditionalMutationFinding;
import io.github.databaseaudits.audit.finding.UniqueIndexNullableColumnFinding;
import io.github.databaseaudits.audit.finding.UnmappedColumnFinding;
import io.github.databaseaudits.audit.finding.UnmappedTableFinding;
import io.github.databaseaudits.audit.finding.UnusedIndexFinding;
import io.github.databaseaudits.platform.DatabasePlatform;

/**
 * Turns a structured {@link Finding} into a {@link Fix} — the DDL (or advisory
 * guidance) that remediates it — and, for Liquibase output, a native
 * {@code <changeSet>} for it.
 *
 * <p>
 * {@link #fixFor(Finding, DatabasePlatform)} is an exhaustive {@code switch} over
 * the sealed finding hierarchy, so adding a finding kind in core is a
 * compile-checked change here. The DDL is platform-aware where the engines
 * diverge — {@code DROP INDEX}, making a column {@code NOT NULL}, and changing a
 * column's type all differ between the MySQL family (MySQL/MariaDB) and the
 * ANSI-style engines (PostgreSQL/H2). Fixes the catalog cannot fully specify
 * (a table's missing primary-key columns, an index's columns inferred from an
 * {@code EXPLAIN} plan) carry a non-{@link FixFidelity#PRECISE} fidelity and a
 * {@code TODO} so they are reviewed before use.
 */
public class FixRenderer {
    private static final String LIQUIBASE_AUTHOR = "database-audits";

    private static final Pattern QUOTED_RELATION = Pattern.compile("'([^']+)'");

    private static final String NULL_BACKFILL_NOTE =
            "-- NOTE: fails if the column currently holds NULLs — backfill first.";

    /**
     * Renders the remediation for a single finding, in the dialect of the given
     * platform.
     *
     * @param finding
     *                     the finding to remediate.
     * @param platform
     *                     the database platform whose DDL dialect to emit.
     * @return the fix for the finding.
     */
    public Fix fixFor(final Finding finding, final DatabasePlatform platform) {
        return switch (finding) {
            case ForeignKeyIndexFinding f -> new Fix(FixFidelity.PRECISE,
                    "CREATE INDEX " + indexName(f.table(), f.columns()) + " ON "
                            + f.table() + " (" + String.join(", ", f.columns())
                            + ");");
            case RedundantIndexFinding f -> new Fix(FixFidelity.PRECISE,
                    dropIndex(platform, f.redundantIndex(), f.table()));
            case ForeignKeyNotNullFinding f -> setNotNull(platform, f.table(),
                    f.column());
            case ForeignKeyTypeMismatchFinding f -> new Fix(FixFidelity.PRECISE,
                    alterColumnType(platform, f.table(), f.column(),
                            f.referencedType()));
            case SchemaColumnMissingFinding f -> new Fix(FixFidelity.PRECISE,
                    "ALTER TABLE " + f.qualifiedTable() + " ADD COLUMN "
                            + f.column() + " " + f.expectedType() + ";");
            case SchemaColumnTypeMismatchFinding f -> new Fix(
                    FixFidelity.PRECISE, alterColumnType(platform,
                            f.qualifiedTable(), f.column(), f.expectedType()));
            case SchemaTableMissingFinding f -> new Fix(FixFidelity.TEMPLATE,
                    "CREATE TABLE " + f.qualifiedTable() + " (\n"
                            + "    /* TODO: define columns to match the entity mapping */\n"
                            + ");");
            case MissingPrimaryKeyFinding f -> new Fix(FixFidelity.TEMPLATE,
                    "ALTER TABLE " + f.table()
                            + " ADD PRIMARY KEY (/* TODO: key column(s) */);");
            case PlanIndexFinding f -> new Fix(FixFidelity.BEST_EFFORT,
                    planIndexSuggestion(f));
            case UnconditionalMutationFinding f -> new Fix(FixFidelity.ADVISORY,
                    "-- Add a WHERE clause to scope this statement:\n-- "
                            + f.statement());
            case DuplicateForeignKeyFinding f -> new Fix(FixFidelity.PRECISE,
                    dropDuplicateConstraintsDdl(platform, f));
            case NarrowPrimaryKeyFinding f -> new Fix(FixFidelity.TEMPLATE,
                    "-- TODO: widen any referencing foreign key columns in the same change.\n"
                            + alterColumnType(platform, f.table(), f.column(),
                                    "BIGINT"));
            case UniqueIndexNullableColumnFinding f -> new Fix(
                    FixFidelity.ADVISORY,
                    "-- Make the nullable column(s) NOT NULL: "
                            + String.join(", ", f.nullableColumns())
                            + "\n-- Or, on PostgreSQL 15+, recreate the index with NULLS NOT DISTINCT.\n"
                            + "-- Or exclude the index if the partial uniqueness is deliberate.");
            case OffsetPaginationFinding f -> new Fix(FixFidelity.ADVISORY,
                    "-- Switch to keyset (seek) pagination:\n"
                            + "-- WHERE (sort_key, id) > (?, ?) ORDER BY sort_key, id LIMIT ?\n"
                            + "-- Statement: " + f.statement());
            case RepeatedStatementFinding f -> new Fix(FixFidelity.ADVISORY,
                    "-- Eliminate the N+1 with a fetch join, @EntityGraph, or "
                            + "@BatchSize/hibernate.default_batch_fetch_size.\n"
                            + "-- Executed " + f.count() + " times: "
                            + f.statement());
            case MissingVersionAttributeFinding f -> new Fix(
                    FixFidelity.ADVISORY,
                    "-- Add a @Version attribute to " + f.entityName()
                            + " (table " + f.table()
                            + "), or exclude it if genuinely append-only or single-writer.");
            case EagerCollectionFetchFinding f -> new Fix(FixFidelity.ADVISORY,
                    "-- Switch " + f.role() + " (collection table "
                            + f.collectionTable() + ") to FetchType.LAZY, and "
                            + "fetch eagerly only where a specific query needs it (fetch join or @EntityGraph).");
            case UnmappedTableFinding f -> new Fix(FixFidelity.ADVISORY,
                    "-- Map " + f.qualifiedTable()
                            + " with an entity, or drop it via a migration, or exclude it.");
            case UnmappedColumnFinding f -> new Fix(FixFidelity.ADVISORY,
                    "-- Map " + f.qualifiedTable() + "." + f.column()
                            + " with an entity, or drop it via a migration, or exclude it."
                            + (f.notNullWithoutDefault()
                                    ? "\n-- NOT NULL with no default: entity inserts against this table currently fail."
                                    : ""));
            case UnusedIndexFinding f -> new Fix(FixFidelity.TEMPLATE,
                    "-- TODO: confirm against production pg_stat_user_indexes before dropping.\n"
                            + dropIndex(platform, f.index(), f.table()));
        };
    }

    /**
     * Renders a finding's fix as a Liquibase change-set fragment. A precise fix
     * becomes a runnable {@code <changeSet>} built from Liquibase's
     * <em>database-agnostic</em> change types ({@code <createIndex>},
     * {@code <dropIndex>}, {@code <modifyDataType>}, {@code <addColumn>},
     * {@code <addNotNullConstraint>}), so Liquibase renders the dialect-correct
     * SQL for whatever database the changelog runs against — this module does not
     * hand-write the DDL here. A fix that cannot be expressed as a runnable change
     * — one whose facts the catalog does not carry, or a nullable-column fix on
     * the MySQL family (whose {@code MODIFY} needs a column type the finding lacks)
     * — is emitted as an XML comment instead, so a changelog assembled from these
     * still applies cleanly. Change-set ids are derived from the finding, so
     * re-runs over the same findings are stable.
     *
     * @param finding
     *                     the finding to remediate.
     * @param platform
     *                     the database platform the audit ran against; used only
     *                     to decide whether a nullable-column fix can be a runnable
     *                     change and to word the commented guidance.
     * @return the change-set (or commented guidance) for the finding.
     */
    public String liquibaseChangeSet(final Finding finding,
            final DatabasePlatform platform) {
        return switch (finding) {
            case ForeignKeyIndexFinding f -> changeSet(finding, createIndex(
                    f.table(), indexName(f.table(), f.columns()), f.columns()));
            case RedundantIndexFinding f -> changeSet(finding,
                    "<dropIndex indexName=\"" + escapeXml(f.redundantIndex())
                            + "\" " + tableAttributes(f.table()) + "/>");
            case ForeignKeyTypeMismatchFinding f -> changeSet(finding,
                    modifyDataType(f.table(), f.column(), f.referencedType()));
            case SchemaColumnMissingFinding f -> changeSet(finding,
                    addColumn(f.qualifiedTable(), f.column(), f.expectedType()));
            case SchemaColumnTypeMismatchFinding f -> changeSet(finding,
                    modifyDataType(f.qualifiedTable(), f.column(),
                            f.expectedType()));
            case ForeignKeyNotNullFinding f -> isMysqlFamily(platform)
                    ? comment(finding, platform)
                    : changeSet(finding,
                            "<!-- " + xmlCommentSafe(NULL_BACKFILL_NOTE)
                                    + " -->\n        " + "<addNotNullConstraint "
                                    + tableAttributes(f.table()) + " columnName=\""
                                    + escapeXml(f.column()) + "\"/>");
            case SchemaTableMissingFinding f -> comment(finding, platform);
            case MissingPrimaryKeyFinding f -> comment(finding, platform);
            case PlanIndexFinding f -> comment(finding, platform);
            case UnconditionalMutationFinding f -> comment(finding, platform);
            case DuplicateForeignKeyFinding f -> changeSet(finding,
                    dropDuplicateConstraintsXml(f));
            case NarrowPrimaryKeyFinding f -> comment(finding, platform);
            case UniqueIndexNullableColumnFinding f -> comment(finding,
                    platform);
            case OffsetPaginationFinding f -> comment(finding, platform);
            case RepeatedStatementFinding f -> comment(finding, platform);
            case MissingVersionAttributeFinding f -> comment(finding,
                    platform);
            case EagerCollectionFetchFinding f -> comment(finding, platform);
            case UnmappedTableFinding f -> comment(finding, platform);
            case UnmappedColumnFinding f -> comment(finding, platform);
            case UnusedIndexFinding f -> comment(finding, platform);
        };
    }

    private static String changeSet(final Finding finding, final String change) {
        return "    <changeSet id=\"" + changeSetId(finding) + "\" author=\""
                + LIQUIBASE_AUTHOR + "\">\n        " + change
                + "\n    </changeSet>";
    }

    private String comment(final Finding finding,
            final DatabasePlatform platform) {
        final Fix fix = fixFor(finding, platform);
        return "    <!-- [" + fix.fidelity().label() + "]\n"
                + xmlCommentSafe(fix.ddl()) + " -->";
    }

    private static String createIndex(final String table, final String name,
            final List<String> columns) {
        final StringBuilder index =
                new StringBuilder("<createIndex indexName=\"")
                        .append(escapeXml(name)).append("\" ")
                        .append(tableAttributes(table)).append(">");
        for (final String column : columns) {
            index.append("\n            <column name=\"")
                    .append(escapeXml(column)).append("\"/>");
        }
        return index.append("\n        </createIndex>").toString();
    }

    private static String addColumn(final String table, final String column,
            final String type) {
        return "<addColumn " + tableAttributes(table)
                + ">\n            <column name=\"" + escapeXml(column)
                + "\" type=\"" + escapeXml(type)
                + "\"/>\n        </addColumn>";
    }

    /**
     * Drops every duplicate constraint but the first (sorted) one, keeping
     * exactly one enforcing the relationship.
     */
    private static String dropDuplicateConstraintsDdl(
            final DatabasePlatform platform,
            final DuplicateForeignKeyFinding finding) {
        final String dropClause =
                isMysqlFamily(platform) ? "DROP FOREIGN KEY" : "DROP CONSTRAINT";
        return finding.constraints().stream().skip(1)
                .map(constraint -> "ALTER TABLE " + finding.table() + " "
                        + dropClause + " " + constraint + ";")
                .collect(Collectors.joining("\n"));
    }

    private static String dropDuplicateConstraintsXml(
            final DuplicateForeignKeyFinding finding) {
        return finding.constraints().stream().skip(1)
                .map(constraint -> "<dropForeignKeyConstraint "
                        + tableAttributes(finding.table())
                        + " constraintName=\"" + escapeXml(constraint)
                        + "\"/>")
                .collect(Collectors.joining("\n        "));
    }

    private static String modifyDataType(final String table,
            final String column, final String newType) {
        return "<modifyDataType " + tableAttributes(table) + " columnName=\""
                + escapeXml(column) + "\" newDataType=\"" + escapeXml(newType)
                + "\"/>";
    }

    private static String tableAttributes(final String qualifiedTable) {
        final int dot = qualifiedTable.lastIndexOf('.');
        if (dot < 0) {
            return "tableName=\"" + escapeXml(qualifiedTable) + "\"";
        }
        return "schemaName=\"" + escapeXml(qualifiedTable.substring(0, dot))
                + "\" tableName=\""
                + escapeXml(qualifiedTable.substring(dot + 1)) + "\"";
    }

    private static Fix setNotNull(final DatabasePlatform platform,
            final String table, final String column) {
        if (isMysqlFamily(platform)) {
            return new Fix(FixFidelity.TEMPLATE, NULL_BACKFILL_NOTE + "\n"
                    + "ALTER TABLE " + table + " MODIFY " + column
                    + " /* TODO: column type */ NOT NULL;");
        }
        return new Fix(FixFidelity.PRECISE, NULL_BACKFILL_NOTE + "\n"
                + "ALTER TABLE " + table + " ALTER COLUMN " + column
                + " SET NOT NULL;");
    }

    private static String dropIndex(final DatabasePlatform platform,
            final String index, final String table) {
        return isMysqlFamily(platform)
                ? "DROP INDEX " + index + " ON " + table + ";"
                : "DROP INDEX " + index + ";";
    }

    private static String alterColumnType(final DatabasePlatform platform,
            final String table, final String column, final String type) {
        return isMysqlFamily(platform)
                ? "ALTER TABLE " + table + " MODIFY " + column + " " + type + ";"
                : "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE "
                        + type + ";";
    }

    private static String planIndexSuggestion(final PlanIndexFinding finding) {
        final Matcher relationMatch =
                QUOTED_RELATION.matcher(finding.planDetail());
        final String relation =
                relationMatch.find() ? relationMatch.group(1) : null;
        final String onTable = relation == null ? "/* TODO: table */" : relation;
        final String name =
                "ix_" + (relation == null ? "TODO" : sanitize(relation))
                        + "_TODO";
        return "-- Best-effort index suggestion — verify the column(s) against the plan.\n"
                + "-- Plan: " + finding.planDetail() + "\n" + "-- Statement: "
                + finding.statement() + "\n" + "CREATE INDEX " + name + " ON "
                + onTable
                + " (/* TODO: column(s) the plan filters, sorts, or joins on */);";
    }

    private static boolean isMysqlFamily(final DatabasePlatform platform) {
        return platform == DatabasePlatform.MYSQL
                || platform == DatabasePlatform.MARIADB;
    }

    private static String indexName(final String table,
            final List<String> columns) {
        return "ix_" + sanitize(table) + "_"
                + String.join("_", columns.stream()
                        .map(FixRenderer::sanitize).toList());
    }

    private static String changeSetId(final Finding finding) {
        final String canonical =
                finding.getClass().getSimpleName() + '|' + finding.description();
        return LIQUIBASE_AUTHOR + "-" + sha256Hex(canonical).substring(0, 16);
    }

    private static String sha256Hex(final String text) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-guaranteed digest (Java Security Standard Algorithm Names).
            throw new IllegalStateException(e);
        }
    }

    private static String sanitize(final String identifier) {
        return identifier.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String escapeXml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String xmlCommentSafe(final String text) {
        return text.replace("--", "- ");
    }
}
