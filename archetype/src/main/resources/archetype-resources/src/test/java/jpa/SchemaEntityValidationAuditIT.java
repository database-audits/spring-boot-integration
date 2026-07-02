package ${package}.jpa;

#set($multi = $dataSourceNames && $dataSourceNames != '' && $dataSourceNames != 'none')
#if($multi)
import java.util.Map;

#end
#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
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
import io.github.databaseaudits.spring.boot.assertion.SchemaEntityValidationAuditAssertion;
#end

/**
 * Validates that every JPA entity mapping matches the schema, reporting all mismatches in one run. It runs under
 * the default {@code ddl-auto=none}: Hibernate's own {@code validate} fails fast on the first mismatch and aborts
 * startup, so this audit walks the mappings against the live schema itself instead.
#if($multi)
 * Parameterized over every datasource in {@code dataSourceNames}; each run resolves that datasource's suite by name.
#end
 */
#if($disabledTests == 'true')
@Disabled("Generated as disabled; remove @Disabled to enable")
#end
#if($multi)
@Import(DatabaseAuditMultiTestConfiguration.class)
#end
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class SchemaEntityValidationAuditIT extends ${simpleParentClass} {
#else
public class SchemaEntityValidationAuditIT extends AbstractDatabaseAuditIT {
#end
#if($multi)
    @Autowired
    private Map<String, DatabaseAuditSuite> suitesByBeanName;
#else
    @Autowired
    private SchemaEntityValidationAuditAssertion schemaEntityValidationAuditAssertion;
#end

#if($multi)
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {#foreach($n in $dataSourceNames.split(","))#set($t=$n.trim())"${t.substring(0,1).toLowerCase()}${t.substring(1)}"#if($foreach.hasNext), #end#end})
    void testEntitiesMatchSchema(String dataSourceName) {
        suitesByBeanName.get(dataSourceName + "DatabaseAuditSuite")
                .schemaEntityValidationAuditAssertion()
                .assertClean();
    }
#else
    @Test
    void testEntitiesMatchSchema() {
        schemaEntityValidationAuditAssertion.assertClean();
    }
#end
}
