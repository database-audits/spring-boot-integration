package ${package}.jpa;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.SchemaEntityValidationAuditAssertion;

/**
 * The {@code ddl-auto=validate} setting makes Hibernate validate entity↔schema at startup; reaching the test means
 * it passed.
 */
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
#if($disabledTests == 'true')
@Disabled("Generated as disabled; remove @Disabled to enable")
#end
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class SchemaEntityValidationAuditIT extends ${simpleParentClass} {
#else
public class SchemaEntityValidationAuditIT extends AbstractDatabaseAuditIT {
#end
    @Autowired
    private SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion;

    @Test
    void testEntitiesMatchSchema() {
        schemaEntityValidationAuditAssertion.assertClean();
    }
}
