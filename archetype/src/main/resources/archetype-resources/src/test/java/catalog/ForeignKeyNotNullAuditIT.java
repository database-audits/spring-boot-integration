package ${package}.catalog;

import java.util.Set;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyNotNullAuditAssertion;

/**
 * Asserts that every foreign key column is NOT NULL, with a place to exclude genuinely-optional ones.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class ForeignKeyNotNullAuditIT extends ${simpleParentClass} {
#else
public class ForeignKeyNotNullAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude genuinely-optional FKs, e.g. Set.of("orders.coupon_id"). */
    private static final Set<String> EXCLUDED_COLUMNS = Set.of();

    @Autowired
    private ForeignKeyNotNullAuditAssertion foreignKeyNotNullAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryForeignKeyColumnIsConfiguredNotNullOrOptional() {
        foreignKeyNotNullAuditAssertion.assertClean(schema, EXCLUDED_COLUMNS);
    }
}
