package io.github.databaseaudits.spring.boot.report;

import java.util.Locale;

/**
 * The consolidated-report output format, selected by the
 * {@code database-audits.report.format} configuration parameter. Each constant
 * is the markup strategy the report renderer drives — how it writes a heading
 * and a verbatim code block — plus the file extension its report is written
 * under.
 */
public enum ReportFormat {
    /** GitHub-flavored Markdown ({@code .md}). */
    MARKDOWN("md") {
        @Override
        String heading(final int level, final String text) {
            return "#".repeat(level) + " " + text;
        }

        @Override
        String codeBlock(final String language, final String content) {
            return "```" + (language == null ? "" : language) + "\n" + content
                    + "\n```";
        }
    },

    /** AsciiDoc ({@code .adoc}). */
    ASCIIDOC("adoc") {
        @Override
        String heading(final int level, final String text) {
            return "=".repeat(level) + " " + text;
        }

        @Override
        String codeBlock(final String language, final String content) {
            final String opener = language == null || language.isEmpty()
                    ? "----"
                    : "[source," + language + "]\n----";
            return opener + "\n" + content + "\n----";
        }
    },

    /** Plain text ({@code .txt}). */
    TEXT("txt") {
        @Override
        String heading(final int level, final String text) {
            if (level >= 3) {
                return text;
            }
            final char rule = level == 1 ? '=' : '-';
            return text + "\n" + String.valueOf(rule).repeat(text.length());
        }

        @Override
        String codeBlock(final String language, final String content) {
            return content;
        }
    };

    private final String fileExtension;

    ReportFormat(final String fileExtension) {
        this.fileExtension = fileExtension;
    }

    /**
     * Returns the file extension (without the dot) a report of this format is
     * written under.
     *
     * @return the file extension.
     */
    public String fileExtension() {
        return fileExtension;
    }

    /**
     * Renders a section heading at the given level in this format.
     *
     * @param level
     *                  the heading level, {@code 1} being the most prominent.
     * @param text
     *                  the heading text.
     * @return the formatted heading.
     */
    abstract String heading(int level, String text);

    /**
     * Renders {@code content} verbatim as a code block in this format,
     * preserving its line breaks.
     *
     * @param language
     *                     the source language for syntax highlighting, or
     *                     {@code null}/empty for a plain block.
     * @param content
     *                     the verbatim block content.
     * @return the formatted code block.
     */
    abstract String codeBlock(String language, String content);

    /**
     * Parses a report-format name, case-insensitively, falling back to
     * {@link #ASCIIDOC} for a {@code null}, blank, or unrecognized value.
     *
     * @param value
     *                  the configured report-format name.
     * @return the matching report format, or {@link #ASCIIDOC}.
     */
    public static ReportFormat from(final String value) {
        if (value == null || value.isBlank()) {
            return ASCIIDOC;
        }
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException unknown) {
            return ASCIIDOC;
        }
    }
}
