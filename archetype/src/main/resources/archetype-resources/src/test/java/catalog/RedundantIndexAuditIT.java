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
import io.github.databaseaudits.spring.boot.assertion.RedundantIndexAuditAssertion;

/**
 * Asserts that the schema has no redundant indexes, with a place to exclude intentional look-alikes.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class RedundantIndexAuditIT extends ${simpleParentClass} {
#else
public class RedundantIndexAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude intentional look-alikes, e.g. Set.of("ix_orders_customer_id"). */
    private static final Set<String> EXCLUDED_INDEXES = Set.of();

    @Autowired
    private RedundantIndexAuditAssertion redundantIndexAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoRedundantIndexes() {
        redundantIndexAuditAssertion.assertClean(schema, EXCLUDED_INDEXES);
    }
}
