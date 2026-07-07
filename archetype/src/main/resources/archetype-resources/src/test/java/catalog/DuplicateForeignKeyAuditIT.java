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
import io.github.databaseaudits.spring.boot.assertion.DuplicateForeignKeyAuditAssertion;

/**
 * Asserts that no relationship in the schema is enforced by more than one foreign key constraint, with a place
 * to exclude a deliberate duplicate.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class DuplicateForeignKeyAuditIT extends ${simpleParentClass} {
#else
public class DuplicateForeignKeyAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude a deliberate duplicate constraint, e.g. Set.of("fk_orders_customer_legacy"). */
    private static final Set<String> EXCLUDED_CONSTRAINTS = Set.of();

    @Autowired
    private DuplicateForeignKeyAuditAssertion duplicateForeignKeyAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoDuplicateForeignKeyConstraints() {
        duplicateForeignKeyAuditAssertion.assertClean(schema, EXCLUDED_CONSTRAINTS);
    }
}
