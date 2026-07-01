package io.github.databaseaudits.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.cfg.JdbcSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import io.github.databaseaudits.capture.SqlCapturingStatementInspector;

/**
 * Verifies that the post-processor sets the named capturer as the named entity-manager factory's
 * {@code StatementInspector} before the factory is built, and leaves every other bean untouched.
 */
class SqlCapturerRegisteringPostProcessorTest {
    private static final String EMF_BEAN = "reportingEntityManagerFactory";

    private static final String CAPTURER_BEAN = "reportingSqlCapturer";

    private final SqlCapturingStatementInspector capturer = new SqlCapturingStatementInspector();

    private final BeanFactory beanFactory = mock(BeanFactory.class);

    private final SqlCapturerRegisteringPostProcessor postProcessor = postProcessor();

    /**
     * The target factory, resolved by bean name, gets the named capturer as its {@code StatementInspector}.
     */
    @Test
    void testPostProcessBeforeInitialization_TargetFactoryByName_SetsCapturerAsStatementInspector() {
        when(beanFactory.getBean(CAPTURER_BEAN, SqlCapturingStatementInspector.class)).thenReturn(capturer);
        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        postProcessor.postProcessBeforeInitialization(factory, EMF_BEAN);

        assertThat(factory.getJpaPropertyMap())
                .as("The target factory's StatementInspector is set to the named capturer instance.")
                .containsEntry(JdbcSettings.STATEMENT_INSPECTOR, capturer);
    }

    /**
     * A factory under any other bean name is left alone, and the capturer is never even resolved.
     */
    @Test
    void testPostProcessBeforeInitialization_DifferentBeanName_LeavesFactoryUntouched() {
        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        postProcessor.postProcessBeforeInitialization(factory, "someOtherEntityManagerFactory");

        assertThat(factory.getJpaPropertyMap())
                .as("A factory under a different bean name keeps its StatementInspector unset.")
                .doesNotContainKey(JdbcSettings.STATEMENT_INSPECTOR);
        verify(beanFactory, never()).getBean(CAPTURER_BEAN, SqlCapturingStatementInspector.class);
    }

    /**
     * A non-factory bean under the target name is returned unchanged (only a
     * {@link LocalContainerEntityManagerFactoryBean} can carry the inspector).
     */
    @Test
    void testPostProcessBeforeInitialization_NonFactoryBeanUnderTargetName_ReturnsBeanUnchanged() {
        final Object bean = new Object();

        assertThat(postProcessor.postProcessBeforeInitialization(bean, EMF_BEAN))
                .as("A non-factory bean under the target name is returned unchanged.").isSameAs(bean);
        verify(beanFactory, never()).getBean(CAPTURER_BEAN, SqlCapturingStatementInspector.class);
    }

    private SqlCapturerRegisteringPostProcessor postProcessor() {
        final SqlCapturerRegisteringPostProcessor processor =
                new SqlCapturerRegisteringPostProcessor(EMF_BEAN, CAPTURER_BEAN);
        processor.setBeanFactory(beanFactory);
        return processor;
    }
}
