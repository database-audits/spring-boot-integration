package ${package}.runtime;

import java.util.Set;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.UnusedIndexAuditAssertion;

/**
 * Asserts that every index is used by at least one captured statement's plan. Advisory and workload-dependent:
 * confirm against production pg_stat_user_indexes before dropping an index this reports.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class UnusedIndexAuditIT extends ${simpleParentClass} {
#else
public class UnusedIndexAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude an index kept for a workload outside this capture, e.g. Set.of("idx_orders_admin_report"). */
    private static final Set<String> EXCLUDED_INDEXES = Set.of();

    @Autowired
    private UnusedIndexAuditAssertion unusedIndexAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryIndexIsUsedByCapturedWorkload() {
        unusedIndexAuditAssertion.assertClean(schema, EXCLUDED_INDEXES);
    }
}
