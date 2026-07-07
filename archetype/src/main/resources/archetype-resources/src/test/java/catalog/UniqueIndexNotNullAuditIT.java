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
import io.github.databaseaudits.spring.boot.assertion.UniqueIndexNotNullAuditAssertion;

/**
 * Asserts that no UNIQUE index in the schema includes a nullable column, with a place to exclude a deliberate
 * partial-uniqueness design.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class UniqueIndexNotNullAuditIT extends ${simpleParentClass} {
#else
public class UniqueIndexNotNullAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude a deliberate partial-uniqueness index, e.g. Set.of("uq_orders_active_customer"). */
    private static final Set<String> EXCLUDED_INDEXES = Set.of();

    @Autowired
    private UniqueIndexNotNullAuditAssertion uniqueIndexNotNullAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoUniqueIndexOverNullableColumn() {
        uniqueIndexNotNullAuditAssertion.assertClean(schema, EXCLUDED_INDEXES);
    }
}
