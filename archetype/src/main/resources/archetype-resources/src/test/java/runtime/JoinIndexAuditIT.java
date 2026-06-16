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
import io.github.databaseaudits.spring.boot.assertion.JoinIndexAuditAssertion;

/**
 * Asserts that every captured join key is served by an index.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class JoinIndexAuditIT extends ${simpleParentClass} {
#else
public class JoinIndexAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude joins with no indexable side, e.g. Set.of("country"). */
    private static final Set<String> EXCLUDED_RELATIONS = Set.of();

    /** Exclude SQL fragments, e.g. List.of("full outer join"). */
    private static final List<String> EXCLUDED_SQL_FRAGMENTS = List.of();

    @Autowired
    private JoinIndexAuditAssertion joinIndexAuditAssertion;

    @Test
    void testEveryJoinKeyIsServedByAnIndex() {
        joinIndexAuditAssertion.assertClean(EXCLUDED_RELATIONS, EXCLUDED_SQL_FRAGMENTS);
    }
}
