package ${package}.catalog;

import java.util.Set;

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
import io.github.databaseaudits.spring.boot.assertion.PrimaryKeyTypeAuditAssertion;

/**
 * Asserts that every primary key in the schema is at least bigint wide, with a place to exclude a genuinely
 * bounded table.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class PrimaryKeyTypeAuditIT extends ${simpleParentClass} {
#else
public class PrimaryKeyTypeAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Liquibase's own bookkeeping table declares a narrow INT primary key; every application table here is
     *  genuinely BIGINT. Add your own genuinely-bounded tables the same way, e.g. "lookup_values.id". */
    private static final Set<String> EXCLUDED_COLUMNS = Set.of("databasechangeloglock.id");

    @Autowired
    private PrimaryKeyTypeAuditAssertion primaryKeyTypeAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryPrimaryKeyIsAtLeastBigint() {
        primaryKeyTypeAuditAssertion.assertClean(schema, EXCLUDED_COLUMNS);
    }
}
