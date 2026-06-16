package ${package};

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application that anchors the example test context. It lives in the base package (the common
 * ancestor of the audit subpackages) so {@code @SpringBootTest} auto-detects it from every audit subpackage
 * ({@code catalog}, {@code jpa}, {@code runtime}), and its component scan registers the demo entities and
 * repositories under {@code app}.
 *
 * <p>
 * It is part of the demo harness that lets the audit ITs run standalone; replace it with your own application to
 * audit your real schema.
 */
@SpringBootApplication
public class DemoApplication {
}
