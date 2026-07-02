package ${package}.catalog;

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
import org.springframework.beans.factory.annotation.Value;
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
import io.github.databaseaudits.spring.boot.assertion.PrimaryKeyPresenceAuditAssertion;
#end

/**
 * Asserts that every base table in the schema has a primary key.
#if($multi)
 * Parameterized over every datasource in {@code dataSourceNames}; each run resolves that datasource's suite by name.
#end
 */
#if($multi)
@Import(DatabaseAuditMultiTestConfiguration.class)
#end
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class PrimaryKeyPresenceAuditIT extends ${simpleParentClass} {
#else
public class PrimaryKeyPresenceAuditIT extends AbstractDatabaseAuditIT {
#end
#if($multi)
    @Autowired
    private Map<String, DatabaseAuditSuite> suitesByBeanName;
#else
    @Autowired
    private PrimaryKeyPresenceAuditAssertion primaryKeyPresenceAuditAssertion;
#end

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

#if($multi)
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {#foreach($n in $dataSourceNames.split(","))#set($t=$n.trim())"${t.substring(0,1).toLowerCase()}${t.substring(1)}"#if($foreach.hasNext), #end#end})
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryBaseTableHasPrimaryKey(String dataSourceName) {
        suitesByBeanName.get(dataSourceName + "DatabaseAuditSuite")
                .primaryKeyPresenceAuditAssertion()
                .assertClean(schema);
    }
#else
    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryBaseTableHasPrimaryKey() {
        primaryKeyPresenceAuditAssertion.assertClean(schema);
    }
#end
}
