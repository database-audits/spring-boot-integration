package ${package}.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts one shared PostgreSQL test container for the JVM and registers its connection as dynamic properties. The
 * JDBC URL carries {@code preferQueryMode=simple} (required by the plan-based runtime audits: generic-plan EXPLAIN
 * of {@code #[[$]]#n} placeholders only works over the simple query protocol). It is component-scanned by
 * {@link DemoApplication}, so every audit context gets the same database. It is part of the demo harness; supply
 * your own {@code DataSource} to audit your real database.
 */
@Configuration(proxyBeanMethods = false)
public class DemoPostgresTestConfig {
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("${postgresImage}"));
    static {
        POSTGRES.withUrlParam("preferQueryMode", "simple");
        POSTGRES.start();
    }

    /**
     * Registers the container's JDBC URL, username, and password so the autoconfigured DataSource connects to it.
     *
     * @return the dynamic property registrar.
     */
    @Bean
    DynamicPropertyRegistrar postgresProperties() {
        return registry -> {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        };
    }
}
