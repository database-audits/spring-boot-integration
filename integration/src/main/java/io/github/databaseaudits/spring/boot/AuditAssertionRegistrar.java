package io.github.databaseaudits.spring.boot;

import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import io.github.databaseaudits.spring.boot.assertion.AuditAssertion;
import io.github.databaseaudits.spring.boot.assertion.DatabaseAuditAssertions;

/**
 * Registers a {@link DatabaseAuditSuite}'s audit assertions — every
 * {@code *AuditAssertion} in {@link DatabaseAuditSuite#all()} plus the
 * {@link DatabaseAuditAssertions} facade — as beans, each under its concrete
 * type, so a configuration wires the whole roster without enumerating one
 * {@code @Bean} method per audit. Adding a core audit then needs no change here:
 * once the suite wires its assertion, this registrar exposes it as a bean.
 *
 * <p>
 * The registration happens in {@link #afterSingletonsInstantiated()}, after the
 * suite bean exists, by adding a bean definition backed by an
 * {@linkplain RootBeanDefinition#setInstanceSupplier(Supplier) instance supplier}
 * for each assertion. Bean names are the assertion's decapitalized simple name
 * (matching the hand-written {@code @Bean} methods this replaces); a non-empty
 * {@code beanNamePrefix} prefixes them instead, so a second, by-name datasource's
 * suite registers its assertions under distinct names without colliding with the
 * primary datasource's. The {@code primary} flag marks the definitions
 * {@link RootBeanDefinition#setPrimary(boolean) primary}, so the primary
 * datasource's assertions win an unqualified by-type injection when an
 * application also audits a peer datasource.
 *
 * <p>
 * The stock {@link DatabaseAuditTestConfiguration} registers one of these with no
 * prefix and {@code primary = true}; a generated per-datasource configuration
 * registers one with the datasource's name prefix and {@code primary = false}.
 */
public class AuditAssertionRegistrar
        implements SmartInitializingSingleton, BeanFactoryAware {
    private final DatabaseAuditSuite suite;

    private final String beanNamePrefix;

    private final boolean primary;

    private BeanDefinitionRegistry registry;

    /**
     * Creates a registrar for one suite's assertions.
     *
     * @param suite
     *                           the suite whose assertions to register.
     * @param beanNamePrefix
     *                           the prefix for the registered bean names, or the
     *                           empty string to name each bean by its
     *                           decapitalized simple name; a non-empty prefix
     *                           keeps a peer datasource's assertions from
     *                           colliding with the primary datasource's.
     * @param primary
     *                           whether the registered beans are marked primary,
     *                           so they win an unqualified by-type injection.
     */
    public AuditAssertionRegistrar(final DatabaseAuditSuite suite,
            final String beanNamePrefix, final boolean primary) {
        this.suite = suite;
        this.beanNamePrefix = beanNamePrefix;
        this.primary = primary;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory)
            throws BeansException {
        if (!(beanFactory instanceof final BeanDefinitionRegistry beanDefinitionRegistry)) {
            throw new IllegalStateException(
                    "AuditAssertionRegistrar requires a BeanDefinitionRegistry-backed BeanFactory (such as a DefaultListableBeanFactory), but got "
                            + beanFactory.getClass().getName());
        }
        this.registry = beanDefinitionRegistry;
    }

    /**
     * Registers a bean for each of the suite's assertions and for its facade,
     * once every singleton — including the suite this registrar depends on — has
     * been instantiated.
     */
    @Override
    public void afterSingletonsInstantiated() {
        for (final AuditAssertion assertion : suite.all()) {
            register(assertion);
        }
        register(suite.assertions());
    }

    private void register(final Object bean) {
        final RootBeanDefinition definition = new RootBeanDefinition();
        definition.setBeanClass(bean.getClass());
        definition.setInstanceSupplier(() -> bean);
        definition.setPrimary(primary);
        registry.registerBeanDefinition(beanName(bean.getClass()), definition);
    }

    private String beanName(final Class<?> type) {
        final String simpleName = type.getSimpleName();
        if (beanNamePrefix.isEmpty()) {
            return Character.toLowerCase(simpleName.charAt(0))
                    + simpleName.substring(1);
        }
        return beanNamePrefix + simpleName;
    }
}
