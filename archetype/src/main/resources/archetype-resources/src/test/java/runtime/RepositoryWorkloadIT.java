package ${package}.runtime;

#if($disabledTests == 'true')
import org.junit.jupiter.api.Disabled;
#end
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
#if($generateMode == 'project')
import org.springframework.beans.factory.annotation.Autowired;
#end

#if($parentClass && $parentClass != '' && $parentClass != 'none')
#set($simpleParentClass = $parentClass.replaceAll('.*\.', ''))
import ${parentClass};
#else
import ${package}.AbstractDatabaseAuditIT;
#end
#if($generateMode == 'project')
import ${package}.app.Child;
import ${package}.app.ChildRepository;
import ${package}.app.Parent;
import ${package}.app.ParentRepository;
#end

/**
 * Primes the shared SQL capturer before the runtime audit ITs run. The {@code @Order(Integer.MIN_VALUE)} makes
 * this the first test class, so its indexable WHERE, ORDER BY, and foreign-key JOIN queries are captured (and the
 * capture is non-empty) by the time the runtime audits read it.
#if($generateMode == 'project')
 * This is part of the demo harness; replace it with your own repository workload so the runtime audits inspect
 * your real SQL.
#else
 * Inject your repositories and call representative query methods here — WHERE, ORDER BY, and JOIN queries are
 * what the runtime audits examine.
#end
 */
@Order(Integer.MIN_VALUE)
#if($parentClass && $parentClass != '' && $parentClass != 'none')
class RepositoryWorkloadIT extends ${simpleParentClass} {
#else
class RepositoryWorkloadIT extends AbstractDatabaseAuditIT {
#end
#if($generateMode == 'project')
    @Autowired
    private ParentRepository parents;

    @Autowired
    private ChildRepository children;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void primeSqlCaptureWithIndexableWorkload() {
        Parent parent = parents.save(new Parent("ACME"));
        children.save(new Child("widget", parent));
        children.findByName("widget");
        children.findByParentIdOrderByName(parent.getId());
        children.findByParentCode("ACME");
    }
#else
    // TODO: @Autowired YourRepository yourRepository;

    @Test
#if($disabledTests == 'true')
    @Disabled("Generated as disabled; remove @Disabled to enable")
#end
    void primeSqlCaptureWithRepositoryWorkload() {
        // TODO: Call your repository methods here to prime the SQL capturer.
        // Include queries that exercise WHERE, ORDER BY, and JOIN conditions.
        // Example:
        //   yourRepository.findAll();
        //   yourRepository.findByName("example");
    }
#end
}
