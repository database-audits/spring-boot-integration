package ${package};

#set($multi = $dataSourceNames && $dataSourceNames != '' && $dataSourceNames != 'none')
import org.springframework.boot.test.context.SpringBootTest;
#if(!$multi)
import org.springframework.context.annotation.Import;

import io.github.databaseaudits.spring.boot.DatabaseAuditTestConfiguration;
#end

#if($multi)
/**
 * Base class for the audit integration tests. It boots a Spring Boot test context; each audit IT
 * {@code @Import}s {@link ${package}.multi.DatabaseAuditMultiTestConfiguration}, which registers a
 * {@code DatabaseAuditSuite} for every datasource. It deliberately does not import
 * {@code DatabaseAuditTestConfiguration}: that configuration audits the single primary
 * {@code DataSource}/{@code EntityManagerFactory} by type, which is ambiguous when an application has several peer
 * datasources. Every datasource here is resolved by name instead, so none needs to be {@code @Primary}.
 *
 * <p>
 * It carries no database or container wiring of its own, so it works unchanged against any application. Point it at
 * your own application and datasources to audit your real schemas.
 */
#else
/**
 * Base class for the audit integration tests. It boots a Spring Boot test context and imports
 * {@link DatabaseAuditTestConfiguration}, which registers every audit bean and wires the shared SQL capturer.
 *
 * <p>
 * It carries no database or container wiring of its own, so it works unchanged against any application: the demo
 * {@code @SpringBootApplication} and {@code DataSource} (a Testcontainers PostgreSQL) supply the database for the
 * audit ITs that extend it. Point it at your own application and {@code DataSource} to audit your real schema.
 */
#end
@SpringBootTest
#if(!$multi)
@Import(DatabaseAuditTestConfiguration.class)
#end
public abstract class AbstractDatabaseAuditIT {
}
