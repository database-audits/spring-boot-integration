package ${package}.runtime;

#set($multi = $dataSourceNames && $dataSourceNames != '' && $dataSourceNames != 'none')
#if($multi)
import java.util.Map;
#end
import java.util.Set;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Order;
#if($multi)
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
#else
import org.junit.jupiter.api.Test;
#end
import org.springframework.beans.factory.annotation.Autowired;
#if($multi)
import org.springframework.context.annotation.Import;
#end

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
#if($multi)
import ${package}.multi.DatabaseAuditMultiTestConfiguration;
import io.github.databaseaudits.spring.boot.DatabaseAuditSuite;
#else
import io.github.databaseaudits.spring.boot.assertion.UnconditionalMutationAuditAssertion;
#end

/**
 * Asserts that no captured statement is a full-table UPDATE or DELETE.
 * Must run after the SQL-priming workload so the capturer is non-empty.
#if($multi)
 * Parameterized over every datasource in {@code dataSourceNames}; each run resolves that datasource's suite by name.
#end
 */
@Order(Integer.MAX_VALUE)
#if($multi)
@Import(DatabaseAuditMultiTestConfiguration.class)
#end
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class UnconditionalMutationAuditIT extends ${simpleParentClass} {
#else
public class UnconditionalMutationAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude a deliberate full-table statement, e.g. {@code Set.of("delete from scratch_buffer")}. */
    private static final Set<String> EXCLUDED_STATEMENTS = Set.of();

#if($multi)
    @Autowired
    private Map<String, DatabaseAuditSuite> suitesByBeanName;
#else
    @Autowired
    private UnconditionalMutationAuditAssertion unconditionalMutationAuditAssertion;
#end

#if($multi)
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {#foreach($n in $dataSourceNames.split(","))#set($t=$n.trim())"${t.substring(0,1).toLowerCase()}${t.substring(1)}"#if($foreach.hasNext), #end#end})
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoUnconditionalMutations(String dataSourceName) {
        suitesByBeanName.get(dataSourceName + "DatabaseAuditSuite")
                .unconditionalMutationAuditAssertion()
                .assertClean(EXCLUDED_STATEMENTS);
    }
#else
    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoUnconditionalMutations() {
        unconditionalMutationAuditAssertion.assertClean(EXCLUDED_STATEMENTS);
    }
#end
}
