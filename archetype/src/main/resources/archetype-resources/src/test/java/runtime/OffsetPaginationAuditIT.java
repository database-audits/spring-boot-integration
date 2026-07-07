package ${package}.runtime;

import java.util.List;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.OffsetPaginationAuditAssertion;

/**
 * Asserts that no captured query paginates with OFFSET.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class OffsetPaginationAuditIT extends ${simpleParentClass} {
#else
public class OffsetPaginationAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude a deliberately shallow/bounded paginated statement, e.g. List.of("from admin_report"). */
    private static final List<String> EXCLUDED_SQL_FRAGMENTS = List.of();

    @Autowired
    private OffsetPaginationAuditAssertion offsetPaginationAuditAssertion;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoQueryPaginatesWithOffset() {
        offsetPaginationAuditAssertion.assertClean(EXCLUDED_SQL_FRAGMENTS);
    }
}
