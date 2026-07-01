package io.github.databaseaudits.spring.boot;

import javax.sql.DataSource;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

import jakarta.persistence.EntityManagerFactory;

/**
 * Matches only when the context can resolve a single {@link DataSource} and a single
 * {@link EntityManagerFactory} unambiguously — that is, when each type has at most one candidate bean, or
 * exactly one candidate marked {@code @Primary}. It mirrors the semantics of Spring Boot's
 * {@code @ConditionalOnSingleCandidate} without depending on {@code spring-boot-autoconfigure}, and runs in the
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} phase so every application bean definition is present
 * before it evaluates.
 *
 * <p>
 * {@link DatabaseAuditTestConfiguration} is gated on this condition so it stays active for a single-datasource
 * application (the common case) but backs off entirely when an application registers several peer datasources with
 * no {@code @Primary}: there its by-type injection of the {@code DataSource}/{@code EntityManagerFactory} would be
 * ambiguous, so importing it must be a no-op rather than a context failure. Such applications audit each datasource
 * through a per-datasource {@code @TestConfiguration} that resolves its beans by name instead.
 */
public class SingleDataSourceCondition implements ConfigurationCondition {
    /**
     * Creates the condition.
     */
    public SingleDataSourceCondition() {
    }

    /**
     * Evaluates in the {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} phase, so all application bean
     * definitions are registered before the datasource candidates are counted.
     *
     * @return {@link ConfigurationPhase#REGISTER_BEAN}.
     */
    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }

    /**
     * Matches when both a {@link DataSource} and an {@link EntityManagerFactory} resolve to a single or
     * {@code @Primary} candidate.
     *
     * @param context
     *                     the condition context.
     * @param metadata
     *                     the metadata of the annotated element.
     * @return {@code true} when both types are unambiguous, {@code false} otherwise.
     */
    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory == null) {
            return true;
        }
        return hasSingleOrPrimaryCandidate(beanFactory, DataSource.class)
                && hasSingleOrPrimaryCandidate(beanFactory, EntityManagerFactory.class);
    }

    private boolean hasSingleOrPrimaryCandidate(final ConfigurableListableBeanFactory beanFactory,
            final Class<?> type) {
        final String[] names = beanFactory.getBeanNamesForType(type, true, false);
        if (names.length <= 1) {
            return true;
        }
        int primaryCount = 0;
        for (final String name : names) {
            if (isPrimary(beanFactory, name)) {
                primaryCount++;
            }
        }
        return primaryCount == 1;
    }

    private boolean isPrimary(final ConfigurableListableBeanFactory beanFactory, final String name) {
        try {
            final BeanDefinition definition = beanFactory.getMergedBeanDefinition(name);
            return definition.isPrimary();
        } catch (final NoSuchBeanDefinitionException ex) {
            return false;
        }
    }
}
