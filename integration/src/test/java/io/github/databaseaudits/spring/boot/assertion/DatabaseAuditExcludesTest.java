package io.github.databaseaudits.spring.boot.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link DatabaseAuditExcludes.Builder}'s validation of
 * {@code repeatedStatementThreshold}.
 */
class DatabaseAuditExcludesTest {

    @Test
    void testRepeatedStatementThreshold_AtLeastTwo_IsStored() {
        final DatabaseAuditExcludes excludes = DatabaseAuditExcludes.builder()
                .repeatedStatementThreshold(2).build();

        assertThat(excludes.repeatedStatementThreshold())
                .as("A threshold of exactly 2 (the documented floor) should be accepted and stored.")
                .isEqualTo(2);
    }

    @Test
    void testRepeatedStatementThreshold_BelowTwo_ThrowsIllegalArgumentException() {
        final DatabaseAuditExcludes.Builder builder =
                DatabaseAuditExcludes.builder();

        assertThatIllegalArgumentException()
                .as("A threshold below the documented floor of 2 must be rejected, not silently accepted.")
                .isThrownBy(() -> builder.repeatedStatementThreshold(1));
    }
}
