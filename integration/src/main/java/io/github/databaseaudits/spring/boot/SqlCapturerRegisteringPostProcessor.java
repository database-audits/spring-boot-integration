package io.github.databaseaudits.spring.boot;

import org.hibernate.cfg.JdbcSettings;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;
import jakarta.persistence.EntityManagerFactory;

/**
 * Registers a {@link SqlCapturingStatementInspector} as a named {@link EntityManagerFactory}'s Hibernate
 * {@code StatementInspector} at test time, without changing the application's own (production) configuration of that
 * factory.
 *
 * <p>
 * Hibernate fixes the {@code StatementInspector} when the {@code EntityManagerFactory} is built, so it cannot be
 * attached afterwards, and Spring Boot's {@code HibernatePropertiesCustomizer} reaches only the auto-configured
 * primary factory — not a peer factory an application builds itself. This post-processor bridges that gap: it runs
 * in {@link #postProcessBeforeInitialization(Object, String)} — before the target
 * {@link LocalContainerEntityManagerFactoryBean} builds its factory — and puts the capturer instance into that
 * factory's JPA property map under {@link JdbcSettings#STATEMENT_INSPECTOR}. The capturer is resolved by name from
 * the {@link BeanFactory} on demand, so it is never created earlier than needed.
 *
 * <p>
 * The stock {@link DatabaseAuditTestConfiguration} does not need this — Spring Boot's customizer wires the primary
 * factory for it. A generated per-datasource {@code DatabaseAudit<Name>TestConfiguration} registers one of these so
 * its runtime audits capture the datasource's SQL with no consumer wiring and no production change. It is a no-op
 * unless the named factory bean is a {@link LocalContainerEntityManagerFactoryBean}; an application that returns a
 * fully-built {@code EntityManagerFactory} from its {@code @Bean} method instead must set the inspector where it
 * builds that factory.
 */
public class SqlCapturerRegisteringPostProcessor
        implements BeanPostProcessor, BeanFactoryAware {
    private final String entityManagerFactoryBeanName;

    private final String sqlCapturerBeanName;

    private BeanFactory beanFactory;

    /**
     * Creates a post-processor that registers {@code sqlCapturerBeanName} as {@code entityManagerFactoryBeanName}'s
     * Hibernate {@code StatementInspector}.
     *
     * @param entityManagerFactoryBeanName
     *                                         the bean name of the {@link LocalContainerEntityManagerFactoryBean}
     *                                         whose {@code StatementInspector} to set.
     * @param sqlCapturerBeanName
     *                                         the bean name of the {@link SqlCapturingStatementInspector} to
     *                                         register — the same instance the datasource's audits read.
     */
    public SqlCapturerRegisteringPostProcessor(final String entityManagerFactoryBeanName,
            final String sqlCapturerBeanName) {
        this.entityManagerFactoryBeanName = entityManagerFactoryBeanName;
        this.sqlCapturerBeanName = sqlCapturerBeanName;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Sets the capturer as the target factory's {@code StatementInspector} just before it is built, leaving every
     * other bean untouched.
     *
     * @param bean
     *                     the bean being initialized.
     * @param beanName
     *                     its bean name.
     * @return the same {@code bean}, unmodified except for the target factory's JPA properties.
     */
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        if (entityManagerFactoryBeanName.equals(beanName)
                && bean instanceof LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            entityManagerFactory.getJpaPropertyMap().put(JdbcSettings.STATEMENT_INSPECTOR,
                    beanFactory.getBean(sqlCapturerBeanName, SqlCapturingStatementInspector.class));
        }
        return bean;
    }
}
