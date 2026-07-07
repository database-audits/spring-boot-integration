package ${package}.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Demo parent entity referenced by {@link Child} through an indexed, not-null, type-matched foreign key. Carries
 * a {@code @Version} attribute so concurrent updates are detected rather than silently overwritten.
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

    @Version
    @Column(name = "version", nullable = false)
    private int version;

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

    /**
     * Returns the optimistic-locking version.
     *
     * @return the version.
     */
    public int getVersion() {
        return version;
    }
}
