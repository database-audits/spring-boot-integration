package ${package}.runtime;

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
import io.github.databaseaudits.spring.boot.assertion.UnconditionalMutationAuditAssertion;

/**
 * Asserts that no captured statement is a full-table UPDATE or DELETE.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class UnconditionalMutationAuditIT extends ${simpleParentClass} {
#else
public class UnconditionalMutationAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude a deliberate full-table statement, e.g. {@code Set.of("delete from scratch_buffer")}. */
    private static final Set<String> EXCLUDED_STATEMENTS = Set.of();

    @Autowired
    private UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion;

    @Test
    void testNoUnconditionalMutations() {
        unconditionalMutationAuditAssertion.assertClean(EXCLUDED_STATEMENTS);
    }
}
