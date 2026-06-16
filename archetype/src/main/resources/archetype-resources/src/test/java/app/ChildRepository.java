package ${package}.app;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data repository for {@link Child}. Its finders deliberately exercise an indexed WHERE column, an indexed
 * ORDER BY column, and a foreign-key JOIN so the runtime plan audits have representative, index-satisfiable SQL to
 * inspect.
 */
public interface ChildRepository extends JpaRepository<Child, Long> {
    /**
     * Finds children by their indexed name column (drives the WHERE-clause audit).
     *
     * @param name the child name.
     * @return the matching children.
     */
    List<Child> findByName(String name);

    /**
     * Finds children of a parent ordered by their indexed name column (drives the WHERE and ORDER BY audits).
     *
     * @param parentId the parent identifier.
     * @return the matching children, ordered by name.
     */
    List<Child> findByParentIdOrderByName(Long parentId);

    /**
     * Finds children by their parent's business code through a foreign-key join (drives the JOIN audit).
     *
     * @param code the parent business code.
     * @return the matching children.
     */
    @Query("select c from Child c join c.parent p where p.code = ?1")
    List<Child> findByParentCode(String code);
}
