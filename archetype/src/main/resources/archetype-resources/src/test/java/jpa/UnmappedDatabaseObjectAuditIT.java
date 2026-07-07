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
import io.github.databaseaudits.spring.boot.assertion.UnmappedDatabaseObjectAuditAssertion;

/**
 * Asserts that every physical base table and column in the live schema is mapped by a JPA entity, with a place
 * to exclude a known, acceptable unmapped relation (e.g. a migration-tool bookkeeping table).
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class UnmappedDatabaseObjectAuditIT extends ${simpleParentClass} {
#else
public class UnmappedDatabaseObjectAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Liquibase's own bookkeeping tables track migrations, not application data, so no entity should map them.
     *  Add your own known, acceptable unmapped relations the same way. */
    private static final Set<String> EXCLUDED_RELATIONS =
            Set.of("databasechangelog", "databasechangeloglock");

    @Autowired
    private UnmappedDatabaseObjectAuditAssertion unmappedDatabaseObjectAuditAssertion;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryDatabaseObjectIsMapped() {
        unmappedDatabaseObjectAuditAssertion.assertClean(EXCLUDED_RELATIONS);
    }
}
