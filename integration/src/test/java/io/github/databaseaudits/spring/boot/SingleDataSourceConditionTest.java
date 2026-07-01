package io.github.databaseaudits.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import jakarta.persistence.EntityManagerFactory;

/**
 * The back-off rule that keeps {@link DatabaseAuditTestConfiguration} inert in a peer-datasource application: the
 * condition matches only when both the {@link DataSource} and the {@link EntityManagerFactory} resolve to a single
 * or {@code @Primary} candidate.
 */
class SingleDataSourceConditionTest {
    private final SingleDataSourceCondition condition = new SingleDataSourceCondition();

    private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    /**
     * The common single-datasource application: one of each type, so the configuration stays active.
     */
    @Test
    void testMatches_OneDataSourceAndOneEntityManagerFactory_Matches() {
        final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBeanNamesForType(DataSource.class, true, false)).thenReturn(new String[] {"dataSource"});
        when(beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false))
                .thenReturn(new String[] {"entityManagerFactory"});

        assertThat(condition.matches(context(beanFactory), metadata))
                .as("A single datasource keeps the stock configuration active.").isTrue();
    }

    /**
     * Several peer datasources with no {@code @Primary}: by-type injection would be ambiguous, so the configuration
     * must back off.
     */
    @Test
    void testMatches_TwoDataSourcesWithNoPrimary_DoesNotMatch() {
        final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBeanNamesForType(DataSource.class, true, false))
                .thenReturn(new String[] {"auroraDataSource", "auroraSecondaryDataSource"});
        when(beanFactory.getMergedBeanDefinition("auroraDataSource")).thenReturn(beanDefinition(false));
        when(beanFactory.getMergedBeanDefinition("auroraSecondaryDataSource")).thenReturn(beanDefinition(false));

        assertThat(condition.matches(context(beanFactory), metadata))
                .as("Peer datasources with no @Primary back the stock configuration off.").isFalse();
    }

    /**
     * Several datasources but exactly one is {@code @Primary}: the configuration audits that primary datasource.
     */
    @Test
    void testMatches_TwoDataSourcesWithOnePrimary_Matches() {
        final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBeanNamesForType(DataSource.class, true, false))
                .thenReturn(new String[] {"auroraDataSource", "auroraSecondaryDataSource"});
        when(beanFactory.getMergedBeanDefinition("auroraDataSource")).thenReturn(beanDefinition(true));
        when(beanFactory.getMergedBeanDefinition("auroraSecondaryDataSource")).thenReturn(beanDefinition(false));
        when(beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false))
                .thenReturn(new String[] {"entityManagerFactory"});

        assertThat(condition.matches(context(beanFactory), metadata))
                .as("A single @Primary datasource keeps the stock configuration active.").isTrue();
    }

    /**
     * A single datasource but several peer {@code EntityManagerFactory} beans with no {@code @Primary}: still
     * ambiguous, so the configuration backs off.
     */
    @Test
    void testMatches_TwoEntityManagerFactoriesWithNoPrimary_DoesNotMatch() {
        final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBeanNamesForType(DataSource.class, true, false)).thenReturn(new String[] {"dataSource"});
        when(beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false))
                .thenReturn(new String[] {"auroraEntityManagerFactory", "auroraSecondaryEntityManagerFactory"});
        when(beanFactory.getMergedBeanDefinition("auroraEntityManagerFactory")).thenReturn(beanDefinition(false));
        when(beanFactory.getMergedBeanDefinition("auroraSecondaryEntityManagerFactory"))
                .thenReturn(beanDefinition(false));

        assertThat(condition.matches(context(beanFactory), metadata))
                .as("Peer entity-manager factories with no @Primary back the stock configuration off.").isFalse();
    }

    /**
     * A context that exposes no {@link ConfigurableListableBeanFactory} cannot count candidates, so the condition
     * matches rather than blocking the configuration.
     */
    @Test
    void testMatches_NullBeanFactory_Matches() {
        assertThat(condition.matches(context(null), metadata))
                .as("A null bean factory leaves the stock configuration active.").isTrue();
    }

    /**
     * A candidate whose merged bean definition cannot be resolved is treated as non-primary: with two peers and no
     * resolvable {@code @Primary}, the configuration backs off.
     */
    @Test
    void testMatches_CandidateWithNoMergedDefinition_TreatedAsNonPrimary() {
        final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBeanNamesForType(DataSource.class, true, false))
                .thenReturn(new String[] {"auroraDataSource", "auroraSecondaryDataSource"});
        when(beanFactory.getMergedBeanDefinition("auroraDataSource"))
                .thenThrow(new NoSuchBeanDefinitionException("auroraDataSource"));
        when(beanFactory.getMergedBeanDefinition("auroraSecondaryDataSource")).thenReturn(beanDefinition(false));

        assertThat(condition.matches(context(beanFactory), metadata))
                .as("A candidate with no merged bean definition is treated as non-primary.").isFalse();
    }

    private static ConditionContext context(final ConfigurableListableBeanFactory beanFactory) {
        final ConditionContext context = mock(ConditionContext.class);
        when(context.getBeanFactory()).thenReturn(beanFactory);
        return context;
    }

    private static BeanDefinition beanDefinition(final boolean primary) {
        final RootBeanDefinition definition = new RootBeanDefinition();
        definition.setPrimary(primary);
        return definition;
    }
}
