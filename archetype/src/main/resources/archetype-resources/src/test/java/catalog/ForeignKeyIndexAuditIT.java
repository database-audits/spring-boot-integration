package ${package}.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
import io.github.databaseaudits.spring.boot.assertion.ForeignKeyIndexAuditAssertion;

/**
 * Asserts that every foreign key in the schema has a supporting index.
 */
#if($parentClass && $parentClass != '' && $parentClass != 'none')
public class ForeignKeyIndexAuditIT extends ${simpleParentClass} {
#else
public class ForeignKeyIndexAuditIT extends AbstractDatabaseAuditIT {
#end
    @Autowired
    private ForeignKeyIndexAuditAssertion foreignKeyIndexAuditAssertion;

    @Value("#[[${]]#${schemaPropertyName}#[[}]]#")
    private String schema;

    @Test
    void testEveryForeignKeyHasSupportingIndex() {
        foreignKeyIndexAuditAssertion.assertClean(schema);
    }
}
