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
import io.github.databaseaudits.spring.boot.assertion.JoinIndexAuditAssertion;

/**
 * Asserts that every captured join key is served by an index.
 * Must run after the SQL-priming workload so the capturer is non-empty.
 */
@Order(Integer.MAX_VALUE)
#if($targeted)
@Import(DatabaseAudit${dsPascal}TestConfiguration.class)
#end
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
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryJoinKeyIsServedByAnIndex() {
        joinIndexAuditAssertion.assertClean(EXCLUDED_RELATIONS, EXCLUDED_SQL_FRAGMENTS);
    }
}
