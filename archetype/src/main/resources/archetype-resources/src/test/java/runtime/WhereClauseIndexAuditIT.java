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
import io.github.databaseaudits.spring.boot.assertion.WhereClauseIndexAuditAssertion;

/**
 * Asserts that every captured WHERE-clause column access is served by an index.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class WhereClauseIndexAuditIT extends ${simpleParentClass} {
#else
public class WhereClauseIndexAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude small/static relations, e.g. Set.of("country"). */
    private static final Set<String> EXCLUDED_RELATIONS = Set.of();

    /** Exclude SQL fragments, e.g. List.of("like ?"). */
    private static final List<String> EXCLUDED_SQL_FRAGMENTS = List.of();

    @Autowired
    private WhereClauseIndexAuditAssertion whereClauseIndexAuditAssertion;

    @Test
    void testEveryWhereClauseColumnIsIndexed() {
        whereClauseIndexAuditAssertion.assertClean(EXCLUDED_RELATIONS, EXCLUDED_SQL_FRAGMENTS);
    }
}
