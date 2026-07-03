package io.github.databaseaudits.spring.boot.assertion;

/**
 * The audit family an {@link AuditAssertion} belongs to, mirroring core's audit
 * packages. The {@link DatabaseAuditAssertions} facade groups its per-family
 * runs ({@code assertCatalogClean}, {@code assertJpaClean},
 * {@code assertRuntimeClean}) by this value.
 */
public enum AuditFamily {
    /** Catalog audits: read {@code information_schema}/{@code pg_catalog} over JDBC. */
    CATALOG,

    /** JPA audits: validate Hibernate mappings against the live schema. */
    JPA,

    /** Runtime audits: read the captured SQL (token-scan and plan-based). */
    RUNTIME
}
