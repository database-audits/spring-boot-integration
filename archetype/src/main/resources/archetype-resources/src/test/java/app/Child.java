package ${package}.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Demo child entity whose {@code parent_id} foreign key is indexed, not null, and type-matched to
 * {@link Parent}'s primary key. Its {@code name} column is indexed so the example WHERE/ORDER BY audits see an
 * indexed access path. Carries a {@code @Version} attribute so concurrent updates are detected rather than
 * silently overwritten.
 */
@Entity
@Table(name = "child")
public class Child {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    /**
     * Constructs an empty instance for JPA.
     */
    protected Child() {
    }

    /**
     * Constructs a child with the given name and parent.
     *
     * @param name   the child name.
     * @param parent the owning parent.
     */
    public Child(final String name, final Parent parent) {
        this.name = name;
        this.parent = parent;
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
     * Returns the child name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the owning parent.
     *
     * @return the parent.
     */
    public Parent getParent() {
        return parent;
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
