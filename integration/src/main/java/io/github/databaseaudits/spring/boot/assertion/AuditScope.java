package io.github.databaseaudits.spring.boot.assertion;

/**
 * The inputs a uniform {@link AuditAssertion#assertClean(AuditScope)} run needs:
 * the schema to inspect (used by the catalog audits; ignored by the JPA and
 * runtime audits, which may receive {@code null}) and the
 * {@link DatabaseAuditExcludes} each assertion draws its own slot from. Lets the
 * {@link DatabaseAuditAssertions} facade drive every audit through one method
 * rather than enumerating them.
 *
 * @param schema
 *                     the schema the catalog audits inspect; may be
 *                     {@code null} for the JPA and runtime audits.
 * @param excludes
 *                     the exclusions each assertion draws its own slot from.
 */
public record AuditScope(String schema, DatabaseAuditExcludes excludes) {
}
