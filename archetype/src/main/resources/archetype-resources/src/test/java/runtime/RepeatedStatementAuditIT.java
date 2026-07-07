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
import io.github.databaseaudits.spring.boot.assertion.RepeatedStatementAuditAssertion;

/**
 * Asserts that no captured SELECT shape ran at least THRESHOLD times (an N+1 statement burst).
 * Must run after the SQL-priming workload so the capturer is non-empty. Counts accumulate for the capturer's
 * whole lifetime, so THRESHOLD is a generous regression tripwire rather than a precise count.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class RepeatedStatementAuditIT extends ${simpleParentClass} {
#else
public class RepeatedStatementAuditIT extends AbstractDatabaseAuditIT {
#end
    /** A generous regression tripwire, not a precise N+1 count. */
    private static final int THRESHOLD = 50;

    /** Exclude a legitimately hot statement, e.g. List.of("from feature_flags"). */
    private static final List<String> EXCLUDED_SQL_FRAGMENTS = List.of();

    @Autowired
    private RepeatedStatementAuditAssertion repeatedStatementAuditAssertion;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoStatementShapeRunsAtLeastThresholdTimes() {
        repeatedStatementAuditAssertion.assertClean(THRESHOLD, EXCLUDED_SQL_FRAGMENTS);
    }
}
