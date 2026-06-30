package ${package}.jpa;

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
import io.github.databaseaudits.spring.boot.assertion.SchemaEntityValidationAuditAssertion;

/**
 * Validates that every JPA entity mapping matches the schema, reporting all mismatches in one run. It runs under
 * the default {@code ddl-auto=none}: Hibernate's own {@code validate} fails fast on the first mismatch and aborts
 * startup, so this audit walks the mappings against the live schema itself instead.
 */
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
