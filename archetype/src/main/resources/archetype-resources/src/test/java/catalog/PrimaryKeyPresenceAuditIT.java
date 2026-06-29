package ${package}.catalog;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.PrimaryKeyPresenceAuditAssertion;

/**
 * Asserts that every base table in the schema has a primary key.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class PrimaryKeyPresenceAuditIT extends ${simpleParentClass} {
#else
public class PrimaryKeyPresenceAuditIT extends AbstractDatabaseAuditIT {
#end
    @Autowired
    private PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryBaseTableHasPrimaryKey() {
        primaryKeyPresenceAuditAssertion.assertClean(schema);
    }
}
