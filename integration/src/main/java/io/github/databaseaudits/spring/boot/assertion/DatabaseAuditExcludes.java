package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;
import java.util.Set;

/**
 * Immutable exclusions for the {@link DatabaseAuditAssertions} facade. Every
 * field defaults to empty (or, for {@code repeatedStatementThreshold}, a
 * generous default); build with {@link #builder()} or use {@link #none()}.
 * The plan exclusions ({@code planRelations}, {@code planSqlFragments}) apply
 * to all three plan-based audits (join, order-by, where); for per-audit
 * exclusions inject the individual {@code *AuditAssertion} beans instead.
 */
public class DatabaseAuditExcludes {
    /**
     * Default {@code RepeatedStatementAudit} threshold — generous enough to
     * act as a regression tripwire rather than a precise N+1 count.
     */
    private static final int DEFAULT_REPEATED_STATEMENT_THRESHOLD = 50;

    private static final DatabaseAuditExcludes NONE = builder().build();

    private final Set<String> duplicateForeignKeyConstraints;
    private final Set<String> eagerCollectionRoles;
    private final Set<String> foreignKeyIndexConstraints;
    private final Set<String> foreignKeyNotNullColumns;
    private final Set<String> foreignKeyTypeMatchColumns;
    private final Set<String> jpaExcludedRelations;
    private final Set<String> missingVersionEntities;
    private final List<String> offsetPaginationSqlFragments;
    private final Set<String> planRelations;
    private final List<String> planSqlFragments;
    private final Set<String> primaryKeyTables;
    private final Set<String> primaryKeyTypeColumns;
    private final Set<String> redundantIndexes;
    private final List<String> repeatedStatementSqlFragments;
    private final int repeatedStatementThreshold;
    private final Set<String> unconditionalMutationStatements;
    private final Set<String> uniqueIndexNotNullIndexes;
    private final Set<String> unmappedDatabaseObjectRelations;
    private final Set<String> unusedIndexes;

    private DatabaseAuditExcludes(final Builder builder) {
        this.duplicateForeignKeyConstraints =
                builder.duplicateForeignKeyConstraints;
        this.eagerCollectionRoles = builder.eagerCollectionRoles;
        this.foreignKeyIndexConstraints = builder.foreignKeyIndexConstraints;
        this.foreignKeyNotNullColumns = builder.foreignKeyNotNullColumns;
        this.foreignKeyTypeMatchColumns = builder.foreignKeyTypeMatchColumns;
        this.jpaExcludedRelations = builder.jpaExcludedRelations;
        this.missingVersionEntities = builder.missingVersionEntities;
        this.offsetPaginationSqlFragments =
                builder.offsetPaginationSqlFragments;
        this.planRelations = builder.planRelations;
        this.planSqlFragments = builder.planSqlFragments;
        this.primaryKeyTables = builder.primaryKeyTables;
        this.primaryKeyTypeColumns = builder.primaryKeyTypeColumns;
        this.redundantIndexes = builder.redundantIndexes;
        this.repeatedStatementSqlFragments =
                builder.repeatedStatementSqlFragments;
        this.repeatedStatementThreshold = builder.repeatedStatementThreshold;
        this.unconditionalMutationStatements =
                builder.unconditionalMutationStatements;
        this.uniqueIndexNotNullIndexes = builder.uniqueIndexNotNullIndexes;
        this.unmappedDatabaseObjectRelations =
                builder.unmappedDatabaseObjectRelations;
        this.unusedIndexes = builder.unusedIndexes;
    }

    /**
     * Returns an empty set of exclusions.
     *
     * @return the empty exclusions.
     */
    public static DatabaseAuditExcludes none() {
        return NONE;
    }

    /**
     * Returns a new builder whose exclusions all default to empty (or, for
     * {@code repeatedStatementThreshold}, the generous default).
     *
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    Set<String> duplicateForeignKeyConstraints() {
        return duplicateForeignKeyConstraints;
    }

    Set<String> eagerCollectionRoles() {
        return eagerCollectionRoles;
    }

    Set<String> foreignKeyIndexConstraints() {
        return foreignKeyIndexConstraints;
    }

    Set<String> foreignKeyNotNullColumns() {
        return foreignKeyNotNullColumns;
    }

    Set<String> foreignKeyTypeMatchColumns() {
        return foreignKeyTypeMatchColumns;
    }

    Set<String> jpaExcludedRelations() {
        return jpaExcludedRelations;
    }

    Set<String> missingVersionEntities() {
        return missingVersionEntities;
    }

    List<String> offsetPaginationSqlFragments() {
        return offsetPaginationSqlFragments;
    }

    Set<String> planRelations() {
        return planRelations;
    }

    List<String> planSqlFragments() {
        return planSqlFragments;
    }

    Set<String> primaryKeyTables() {
        return primaryKeyTables;
    }

    Set<String> primaryKeyTypeColumns() {
        return primaryKeyTypeColumns;
    }

    Set<String> redundantIndexes() {
        return redundantIndexes;
    }

    List<String> repeatedStatementSqlFragments() {
        return repeatedStatementSqlFragments;
    }

    int repeatedStatementThreshold() {
        return repeatedStatementThreshold;
    }

    Set<String> unconditionalMutationStatements() {
        return unconditionalMutationStatements;
    }

    Set<String> uniqueIndexNotNullIndexes() {
        return uniqueIndexNotNullIndexes;
    }

    Set<String> unmappedDatabaseObjectRelations() {
        return unmappedDatabaseObjectRelations;
    }

    Set<String> unusedIndexes() {
        return unusedIndexes;
    }

    /**
     * Builder for {@link DatabaseAuditExcludes}.
     */
    public static final class Builder {
        private Set<String> duplicateForeignKeyConstraints = Set.of();
        private Set<String> eagerCollectionRoles = Set.of();
        private Set<String> foreignKeyIndexConstraints = Set.of();
        private Set<String> foreignKeyNotNullColumns = Set.of();
        private Set<String> foreignKeyTypeMatchColumns = Set.of();
        private Set<String> jpaExcludedRelations = Set.of();
        private Set<String> missingVersionEntities = Set.of();
        private List<String> offsetPaginationSqlFragments = List.of();
        private Set<String> planRelations = Set.of();
        private List<String> planSqlFragments = List.of();
        private Set<String> primaryKeyTables = Set.of();
        private Set<String> primaryKeyTypeColumns = Set.of();
        private Set<String> redundantIndexes = Set.of();
        private List<String> repeatedStatementSqlFragments = List.of();
        private int repeatedStatementThreshold =
                DEFAULT_REPEATED_STATEMENT_THRESHOLD;
        private Set<String> unconditionalMutationStatements = Set.of();
        private Set<String> uniqueIndexNotNullIndexes = Set.of();
        private Set<String> unmappedDatabaseObjectRelations = Set.of();
        private Set<String> unusedIndexes = Set.of();

        /** Creates a builder with every exclusion defaulting to empty. */
        public Builder() {
        }

        /**
         * Excludes constraints from the duplicate-foreign-key audit, leaving
         * the duplication in place deliberately.
         *
         * @param constraints
         *                        the FK constraint names to exclude.
         * @return this builder.
         */
        public Builder duplicateForeignKeyConstraints(
                final Set<String> constraints) {
            this.duplicateForeignKeyConstraints = constraints;
            return this;
        }

        /**
         * Excludes deliberately-eager collection roles from the
         * eager-collection-fetch audit.
         *
         * @param roles
         *                  the collection roles to exclude (e.g.
         *                  {@code com.acme.Order.items}).
         * @return this builder.
         */
        public Builder eagerCollectionRoles(final Set<String> roles) {
            this.eagerCollectionRoles = roles;
            return this;
        }

        /**
         * Excludes intentionally-unindexed FK constraints from the foreign-key
         * index audit.
         *
         * @param constraints
         *                        the FK constraint names to exclude.
         * @return this builder.
         */
        public Builder foreignKeyIndexConstraints(
                final Set<String> constraints) {
            this.foreignKeyIndexConstraints = constraints;
            return this;
        }

        /**
         * Excludes genuinely-optional foreign key columns from the not-null
         * audit.
         *
         * @param columns
         *                    the {@code table.column} names to exclude.
         * @return this builder.
         */
        public Builder foreignKeyNotNullColumns(final Set<String> columns) {
            this.foreignKeyNotNullColumns = columns;
            return this;
        }

        /**
         * Excludes deliberate type mismatches from the foreign-key type-match
         * audit.
         *
         * @param columns
         *                    the {@code table.column} names to exclude.
         * @return this builder.
         */
        public Builder foreignKeyTypeMatchColumns(final Set<String> columns) {
            this.foreignKeyTypeMatchColumns = columns;
            return this;
        }

        /**
         * Excludes relations (tables or {@code table.column} pairs) from the
         * JPA schema/entity validation audit.
         *
         * @param relations
         *                      the relation names to exclude, matched
         *                      case-insensitively.
         * @return this builder.
         */
        public Builder jpaExcludedRelations(final Set<String> relations) {
            this.jpaExcludedRelations = relations;
            return this;
        }

        /**
         * Excludes append-only or single-writer entities from the
         * missing-version-attribute audit.
         *
         * @param entities
         *                     the entities to exclude — fully-qualified name,
         *                     simple name, or physical table name.
         * @return this builder.
         */
        public Builder missingVersionEntities(final Set<String> entities) {
            this.missingVersionEntities = entities;
            return this;
        }

        /**
         * Excludes deliberately shallow/bounded paginated statements from the
         * offset-pagination audit.
         *
         * @param sqlFragments
         *                         the SQL fragments to exclude.
         * @return this builder.
         */
        public Builder offsetPaginationSqlFragments(
                final List<String> sqlFragments) {
            this.offsetPaginationSqlFragments = sqlFragments;
            return this;
        }

        /**
         * Excludes relations from all three plan-based audits (join, order-by,
         * where).
         *
         * @param relations
         *                      the relation names to exclude.
         * @return this builder.
         */
        public Builder planRelations(final Set<String> relations) {
            this.planRelations = relations;
            return this;
        }

        /**
         * Excludes statements containing any of these SQL fragments from all
         * three plan-based audits.
         *
         * @param sqlFragments
         *                         the SQL fragments to exclude.
         * @return this builder.
         */
        public Builder planSqlFragments(final List<String> sqlFragments) {
            this.planSqlFragments = sqlFragments;
            return this;
        }

        /**
         * Excludes tables from the primary-key presence audit (the Liquibase
         * bookkeeping tables are always excluded in addition to these).
         *
         * @param tables
         *                   the table names to exclude.
         * @return this builder.
         */
        public Builder primaryKeyTables(final Set<String> tables) {
            this.primaryKeyTables = tables;
            return this;
        }

        /**
         * Excludes genuinely-bounded tables' primary key columns from the
         * primary-key-type audit.
         *
         * @param columns
         *                    the {@code table.column} names to exclude.
         * @return this builder.
         */
        public Builder primaryKeyTypeColumns(final Set<String> columns) {
            this.primaryKeyTypeColumns = columns;
            return this;
        }

        /**
         * Excludes intentional look-alike indexes from the redundant-index
         * audit.
         *
         * @param indexes
         *                    the index names to exclude.
         * @return this builder.
         */
        public Builder redundantIndexes(final Set<String> indexes) {
            this.redundantIndexes = indexes;
            return this;
        }

        /**
         * Excludes legitimately-hot statements from the repeated-statement
         * (N+1) audit.
         *
         * @param sqlFragments
         *                         the SQL fragments to exclude.
         * @return this builder.
         */
        public Builder repeatedStatementSqlFragments(
                final List<String> sqlFragments) {
            this.repeatedStatementSqlFragments = sqlFragments;
            return this;
        }

        /**
         * Sets the minimum capture count (inclusive) for a SELECT shape to be
         * reported by the repeated-statement (N+1) audit; must be at least 2.
         * Defaults to {@value #DEFAULT_REPEATED_STATEMENT_THRESHOLD}, a
         * generous regression tripwire rather than a precise count.
         *
         * @param threshold
         *                      the minimum capture count.
         * @return this builder.
         * @throws IllegalArgumentException
         *                                      if {@code threshold} is less
         *                                      than 2.
         */
        public Builder repeatedStatementThreshold(final int threshold) {
            if (threshold < 2) {
                throw new IllegalArgumentException(
                        "repeatedStatementThreshold must be at least 2, was "
                                + threshold);
            }
            this.repeatedStatementThreshold = threshold;
            return this;
        }

        /**
         * Excludes deliberate full-table statements from the
         * unconditional-mutation audit.
         *
         * @param statements
         *                       the statements to exclude, matched
         *                       case-insensitively against the normalized
         *                       statement text.
         * @return this builder.
         */
        public Builder unconditionalMutationStatements(
                final Set<String> statements) {
            this.unconditionalMutationStatements = statements;
            return this;
        }

        /**
         * Excludes indexes with deliberate partial uniqueness from the
         * unique-index-not-null audit.
         *
         * @param indexes
         *                    the index names to exclude.
         * @return this builder.
         */
        public Builder uniqueIndexNotNullIndexes(final Set<String> indexes) {
            this.uniqueIndexNotNullIndexes = indexes;
            return this;
        }

        /**
         * Excludes known, acceptable unmapped relations (tables or
         * {@code table.column} pairs, optionally schema-qualified) from the
         * unmapped-database-object audit — for example migration-tool
         * bookkeeping tables.
         *
         * @param relations
         *                      the relation names to exclude, matched
         *                      case-insensitively.
         * @return this builder.
         */
        public Builder unmappedDatabaseObjectRelations(
                final Set<String> relations) {
            this.unmappedDatabaseObjectRelations = relations;
            return this;
        }

        /**
         * Excludes indexes kept for a workload outside the captured one (e.g.
         * a rare admin query) from the unused-index audit.
         *
         * @param indexes
         *                    the index names to exclude.
         * @return this builder.
         */
        public Builder unusedIndexes(final Set<String> indexes) {
            this.unusedIndexes = indexes;
            return this;
        }

        /**
         * Builds the exclusions.
         *
         * @return the exclusions.
         */
        public DatabaseAuditExcludes build() {
            return new DatabaseAuditExcludes(this);
        }
    }
}
