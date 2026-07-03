package ${package};

#set($targeted = $dataSourceName && $dataSourceName != '' && $dataSourceName != 'none')
#if($targeted)
#set($dsPascal = "${dataSourceName.substring(0,1).toUpperCase()}${dataSourceName.substring(1)}")
#end
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

#if($targeted)
import ${package}.DatabaseAudit${dsPascal}TestConfiguration;
#else
import io.github.databaseaudits.spring.boot.DatabaseAuditTestConfiguration;
#end

#if($targeted)
/**
 * Base class for the audit integration tests. It boots a Spring Boot test context and {@code @Import}s the generated
 * per-datasource {@code DatabaseAudit<Name>TestConfiguration}, which resolves that datasource's beans by name and
 * registers every audit bean; every audit IT extends this class and inherits that import. It deliberately does not
 * import {@code DatabaseAuditTestConfiguration}: that configuration resolves the
 * {@code DataSource}/{@code EntityManagerFactory} by type, which cannot pick one among several peer datasources.
 *
 * <p>
 * It carries no database or container wiring of its own, so it works unchanged against any application. Point it at
 * your own application and datasource to audit your real schema.
 */
#else
/**
 * Base class for the audit integration tests. It boots a Spring Boot test context and imports
 * {@link DatabaseAuditTestConfiguration}, which registers every audit bean and wires the shared SQL capturer.
 *
 * <p>
 * It carries no database or container wiring of its own, so it works unchanged against any application: the demo
 * {@code @SpringBootApplication} and {@code DataSource} (a Testcontainers database) supply the database for the
 * audit ITs that extend it. Point it at your own application and {@code DataSource} to audit your real schema.
 */
#end
@SpringBootTest
#if($targeted)
@Import(DatabaseAudit${dsPascal}TestConfiguration.class)
#else
@Import(DatabaseAuditTestConfiguration.class)
#end
public abstract class AbstractDatabaseAuditIT {
}
