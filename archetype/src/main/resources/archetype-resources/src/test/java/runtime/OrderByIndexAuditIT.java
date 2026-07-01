package ${package}.runtime;

#set($targeted = $dataSourceName && $dataSourceName != '' && $dataSourceName != 'none')
#if($targeted)
#set($dsPascal = "${dataSourceName.substring(0,1).toUpperCase()}${dataSourceName.substring(1)}")
#end
import java.util.List;
import java.util.Set;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
#if($targeted)
import org.springframework.context.annotation.Import;
#end

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
#if($targeted)
import ${package}.DatabaseAudit${dsPascal}TestConfiguration;
#end
import io.github.databaseaudits.spring.boot.assertion.OrderByIndexAuditAssertion;

/**
 * Asserts that every captured ORDER BY is served by an index, not an explicit sort.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($targeted)
@Import(DatabaseAudit${dsPascal}TestConfiguration.class)
#end
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
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryOrderByIsServedByAnIndexNotAnExplicitSort() {
        orderByIndexAuditAssertion.assertClean(EXCLUDED_RELATIONS, EXCLUDED_SQL_FRAGMENTS);
    }
}
