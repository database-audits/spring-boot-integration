package ${package}.jpa;

import java.util.Set;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.EagerCollectionFetchAuditAssertion;

/**
 * Asserts that no mapped collection is fetched eagerly, with a place to exclude a deliberately eager one.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class EagerCollectionFetchAuditIT extends ${simpleParentClass} {
#else
public class EagerCollectionFetchAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude a deliberately eager collection role, e.g. Set.of("com.example.demo.Order.items"). */
    private static final Set<String> EXCLUDED_ROLES = Set.of();

    @Autowired
    private EagerCollectionFetchAuditAssertion eagerCollectionFetchAuditAssertion;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testNoCollectionIsFetchedEagerly() {
        eagerCollectionFetchAuditAssertion.assertClean(EXCLUDED_ROLES);
    }
}
