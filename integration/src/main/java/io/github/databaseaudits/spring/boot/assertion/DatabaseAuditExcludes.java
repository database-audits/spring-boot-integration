package io.github.databaseaudits.spring.boot.assertion;

import java.util.List;
import java.util.Set;

/**
 * Immutable exclusions for the {@link DatabaseAuditAssertions} facade. Every
 * field defaults to empty; build with {@link #builder()} or use
 * {@link #none()}. The plan exclusions ({@code planRelations},
 * {@code planSqlFragments}) apply to all three plan-based audits (join,
 * order-by, where); for per-audit exclusions inject the individual
 * {@code *AuditAssertion} beans instead.
 */
public class DatabaseAuditExcludes {
    private static final DatabaseAuditExcludes NONE = builder().build();

    private final Set<String> foreignKeyIndexConstraints;
    private final Set<String> foreignKeyNotNullColumns;
    private final Set<String> foreignKeyTypeMatchColumns;
    private final Set<String> jpaExcludedRelations;
    private final Set<String> planRelations;
    private final List<String> planSqlFragments;
    private final Set<String> primaryKeyTables;
    private final Set<String> redundantIndexes;
    private final Set<String> unconditionalMutationStatements;

    private DatabaseAuditExcludes(final Builder builder) {
        this.foreignKeyIndexConstraints = builder.foreignKeyIndexConstraints;
        this.foreignKeyNotNullColumns = builder.foreignKeyNotNullColumns;
        this.foreignKeyTypeMatchColumns = builder.foreignKeyTypeMatchColumns;
        this.jpaExcludedRelations = builder.jpaExcludedRelations;
        this.planRelations = builder.planRelations;
        this.planSqlFragments = builder.planSqlFragments;
        this.primaryKeyTables = builder.primaryKeyTables;
        this.redundantIndexes = builder.redundantIndexes;
        this.unconditionalMutationStatements =
                builder.unconditionalMutationStatements;
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
     * Returns a new builder whose exclusions all default to empty.
     *
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder();
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

    Set<String> planRelations() {
        return planRelations;
    }

    List<String> planSqlFragments() {
        return planSqlFragments;
    }

    Set<String> primaryKeyTables() {
        return primaryKeyTables;
    }

    Set<String> redundantIndexes() {
        return redundantIndexes;
    }

    Set<String> unconditionalMutationStatements() {
        return unconditionalMutationStatements;
    }

    /**
     * Builder for {@link DatabaseAuditExcludes}.
     */
    public static final class Builder {
        private Set<String> foreignKeyIndexConstraints = Set.of();
        private Set<String> foreignKeyNotNullColumns = Set.of();
        private Set<String> foreignKeyTypeMatchColumns = Set.of();
        private Set<String> jpaExcludedRelations = Set.of();
        private Set<String> planRelations = Set.of();
        private List<String> planSqlFragments = List.of();
        private Set<String> primaryKeyTables = Set.of();
        private Set<String> redundantIndexes = Set.of();
        private Set<String> unconditionalMutationStatements = Set.of();

        /** Creates a builder with every exclusion defaulting to empty. */
        public Builder() {
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
         * Builds the exclusions.
         *
         * @return the exclusions.
         */
        public DatabaseAuditExcludes build() {
            return new DatabaseAuditExcludes(this);
        }
    }
}
