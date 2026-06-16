package ${package}.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Demo parent entity referenced by {@link Child} through an indexed, not-null, type-matched foreign key.
 */
@Entity
@Table(name = "parent")
public class Parent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    /**
     * Constructs an empty instance for JPA.
     */
    protected Parent() {
    }

    /**
     * Constructs a parent with the given business code.
     *
     * @param code the business code.
     */
    public Parent(final String code) {
        this.code = code;
    }

    /**
     * Returns the generated identifier.
     *
     * @return the identifier.
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the business code.
     *
     * @return the business code.
     */
    public String getCode() {
        return code;
    }
}
