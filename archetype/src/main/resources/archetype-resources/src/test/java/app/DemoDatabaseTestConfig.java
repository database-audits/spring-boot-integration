package ${package}.app;

#set($customImage = false)
#if($databaseImage && $databaseImage != 'none')
#set($customImage = true)
#set($image = $databaseImage)
#elseif($databasePlatform == 'mysql')
#set($image = 'mysql:8')
#elseif($databasePlatform == 'mariadb')
#set($image = 'mariadb:11')
#else
#set($image = 'postgres:16')
#end
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
#if($databasePlatform == 'mysql')
import org.testcontainers.mysql.MySQLContainer;
#elseif($databasePlatform == 'mariadb')
import org.testcontainers.mariadb.MariaDBContainer;
#else
import org.testcontainers.postgresql.PostgreSQLContainer;
#end
import org.testcontainers.utility.DockerImageName;

/**
 * Starts one shared ${databasePlatform} test container for the JVM and registers its connection as dynamic
 * properties.
#if($databasePlatform == 'postgresql')
 * The JDBC URL carries {@code preferQueryMode=simple} (required by the plan-based runtime audits: generic-plan EXPLAIN
 * of {@code #[[$]]#n} placeholders only works over the simple query protocol).
#end
 * It is component-scanned by {@link DemoApplication}, so every audit context gets the same database. It is part of
 * the demo harness; supply your own {@code DataSource} to audit your real database.
 */
@Configuration(proxyBeanMethods = false)
public class DemoDatabaseTestConfig {
#if($databasePlatform == 'mysql')
    private static final MySQLContainer DATABASE =
            new MySQLContainer(DockerImageName.parse("${image}")#if($customImage).asCompatibleSubstituteFor("mysql")#end);
    static {
        DATABASE.start();
    }
#elseif($databasePlatform == 'mariadb')
    private static final MariaDBContainer DATABASE =
            new MariaDBContainer(DockerImageName.parse("${image}")#if($customImage).asCompatibleSubstituteFor("mariadb")#end);
    static {
        DATABASE.start();
    }
#else
    private static final PostgreSQLContainer DATABASE =
            new PostgreSQLContainer(DockerImageName.parse("${image}")#if($customImage).asCompatibleSubstituteFor("postgres")#end);
    static {
        DATABASE.withUrlParam("preferQueryMode", "simple");
        DATABASE.start();
    }
#end

    /**
     * Registers the container's JDBC URL, username, and password so the autoconfigured DataSource connects to it.
     *
     * @return the dynamic property registrar.
     */
    @Bean
    DynamicPropertyRegistrar databaseProperties() {
        return registry -> {
            registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
            registry.add("spring.datasource.username", DATABASE::getUsername);
            registry.add("spring.datasource.password", DATABASE::getPassword);
        };
    }
}
