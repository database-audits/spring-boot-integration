package ${package};

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.github.databaseaudits.spring.boot.DatabaseAuditTestConfiguration;

/**
 * Base class for the audit integration tests. It boots a Spring Boot test context and imports
 * {@link DatabaseAuditTestConfiguration}, which registers every audit bean and wires the shared SQL capturer.
 *
 * <p>
 * It carries no database or container wiring of its own, so it works unchanged against any application: the demo
 * {@code @SpringBootApplication} and {@code DataSource} (a Testcontainers PostgreSQL) supply the database for the
 * audit ITs that extend it. Point it at your own application and {@code DataSource} to audit your real schema.
 */
@SpringBootTest
@Import(DatabaseAuditTestConfiguration.class)
public abstract class AbstractDatabaseAuditIT {
}
