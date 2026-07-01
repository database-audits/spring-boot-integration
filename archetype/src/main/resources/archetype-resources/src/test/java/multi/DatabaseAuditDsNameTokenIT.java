package ${package}.multi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;

/**
 * Scaffold example that audits the DsNameToken datasource, using {@link DatabaseAuditDsNameTokenTestConfiguration}.
 * The default audit beans (from the imported {@code DatabaseAuditTestConfiguration}) audit the primary datasource;
 * the {@code @Qualifier} here selects the DsNameToken datasource's facade.
 *
 * <p>
 * Generated {@code @Disabled} because it cannot run until you wire the DsNameToken datasource: fill in the
 * {@code @Qualifier} bean names in {@link DatabaseAuditDsNameTokenTestConfiguration}, set the schema below, then
 * remove {@code @Disabled}.
 */
@Disabled("Scaffold: wire the DsNameToken datasource and fill in the @Qualifiers in DatabaseAuditDsNameTokenTestConfiguration, then remove @Disabled.")
@Import(DatabaseAuditDsNameTokenTestConfiguration.class)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class DatabaseAuditDsNameTokenIT extends ${simpleParentClass} {
#else
public class DatabaseAuditDsNameTokenIT extends AbstractDatabaseAuditIT {
#end
    @Autowired
    @Qualifier("dsNameTokenDatabaseAuditAssertions")
    private DatabaseAuditAssertions dsNameTokenAudits;

    // TODO: this injects the primary datasource's schema name property, the only schema property the archetype
    //       knows. Point @Value at the DsNameToken datasource's own schema property if it differs.
    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schemaName;

    @Test
    void testDsNameTokenSchemaIsClean() {
        dsNameTokenAudits.assertCatalogClean(schemaName);
        dsNameTokenAudits.assertJpaClean();
    }
}
