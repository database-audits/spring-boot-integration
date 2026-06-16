package ${package}.app;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Parent}.
 */
public interface ParentRepository extends JpaRepository<Parent, Long> {
}
