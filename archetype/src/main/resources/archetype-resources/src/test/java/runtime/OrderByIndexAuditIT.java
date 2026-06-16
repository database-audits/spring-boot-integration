package ${package}.runtime;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.OrderByIndexAuditAssertion;

/**
 * Asserts that every captured ORDER BY is served by an index, not an explicit sort.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class OrderByIndexAuditIT extends ${simpleParentClass} {
#else
public class OrderByIndexAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude in-memory-sort relations, e.g. Set.of("audit_log"). */
    private static final Set<String> EXCLUDED_RELATIONS = Set.of();

    /** Exclude SQL fragments, e.g. List.of("order by count("). */
    private static final List<String> EXCLUDED_SQL_FRAGMENTS = List.of();

    @Autowired
    private OrderByIndexAuditAssertion orderByIndexAuditAssertion;

    @Test
    void testEveryOrderByIsServedByAnIndexNotAnExplicitSort() {
        orderByIndexAuditAssertion.assertClean(EXCLUDED_RELATIONS, EXCLUDED_SQL_FRAGMENTS);
    }
}
