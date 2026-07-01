package ${package}.catalog;

#set($targeted = $dataSourceName && $dataSourceName != '' && $dataSourceName != 'none')
#if($targeted)
#set($dsPascal = "${dataSourceName.substring(0,1).toUpperCase()}${dataSourceName.substring(1)}")
#end
import java.util.Set;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyTypeMatchAuditAssertion;

/**
 * Asserts that every foreign key column's type matches its referenced column, with a place to exclude deliberate
 * mismatches.
 */
#if($targeted)
@Import(DatabaseAudit${dsPascal}TestConfiguration.class)
#end
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class ForeignKeyTypeMatchAuditIT extends ${simpleParentClass} {
#else
public class ForeignKeyTypeMatchAuditIT extends AbstractDatabaseAuditIT {
#end
    /** Exclude deliberate type mismatches, e.g. Set.of("orders.legacy_id"). */
    private static final Set<String> EXCLUDED_COLUMNS = Set.of();

    @Autowired
    private ForeignKeyTypeMatchAuditAssertion foreignKeyTypeMatchAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void testEveryForeignKeyColumnTypeMatchesItsReferencedColumn() {
        foreignKeyTypeMatchAuditAssertion.assertClean(schema, EXCLUDED_COLUMNS);
    }
}
