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
import io.github.databaseaudits.spring.boot.assertion.MissingVersionAttributeAuditAssertion;

/**
 * Asserts that every mutable root entity carries a {@code @Version} attribute, with a place to exclude a
 * genuinely append-only or single-writer entity.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class MissingVersionAttributeAuditIT extends ${simpleParentClass} {
#else
public class MissingVersionAttributeAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude an append-only or single-writer entity, e.g. Set.of("com.example.demo.AuditLogEntry"). */
    private static final Set<String> EXCLUDED_ENTITIES = Set.of();

    @Autowired
    private MissingVersionAttributeAuditAssertion missingVersionAttributeAuditAssertion;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryMutableEntityIsVersioned() {
        missingVersionAttributeAuditAssertion.assertClean(EXCLUDED_ENTITIES);
    }
}
