/*
 * SPDX-FileCopyrightText: Copyright (c) 2009-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package com.rultor.spi;

import com.jcabi.aspects.Immutable;
import com.jcabi.xml.StrictXML;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSL;
import com.jcabi.xml.XSLChain;
import com.jcabi.xml.XSLDocument;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.cactoos.text.Joined;
import org.w3c.dom.Node;
import org.xembly.Directive;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;

/**
 * Talk.
 *
 * @since 1.0
 */
@Immutable
@SuppressWarnings({"PMD.TooManyMethods",
    "PMD.OnlyOneConstructorShouldDoInitialization",
    "PMD.ConstructorOnlyInitializesOrCallOtherConstructors"})
public interface Talk {

    /**
     * Test name.
     */
    String TEST_NAME = "test";

    /**
     * Schema.
     */
    XML SCHEMA = XMLDocument.make(
        Objects.requireNonNull(Talk.class.getResource("talk.xsd"))
    );

    /**
     * Upgrade XSL.
     */
    XSL UPGRADE = new XSLChain(
        Arrays.asList(
            XSLDocument.make(
                Objects.requireNonNull(
                    Talk.class.getResource("upgrade/001-talks.xsl")
                )
            ),
            XSLDocument.make(
                Objects.requireNonNull(
                    Talk.class.getResource("upgrade/002-public-attribute.xsl")
                )
            )
        )
    );

    /**
     * Its unique number.
     * @return Its number
     * @throws IOException If fails
     * @since 1.3
     */
    Long number() throws IOException;

    /**
     * Its unique name.
     * @return Its name
     * @throws IOException If fails
     */
    String name() throws IOException;

    /**
     * When was it updated.
     * @return When
     * @throws IOException If fails
     */
    Date updated() throws IOException;

    /**
     * Read its content.
     * @return Content
     * @throws IOException If fails
     */
    XML read() throws IOException;

    /**
     * Modify its content.
     * @param dirs Directives
     * @throws IOException If fails
     */
    void modify(Iterable<Directive> dirs) throws IOException;

    /**
     * Make it active or passive.
     * @param yes TRUE if it should be active
     * @throws IOException If fails
     */
    void active(boolean yes) throws IOException;

    /**
     * In file.
     *
     * @since 1.0
     */
    @Immutable
    final class InFile implements Talk {
        /**
         * File.
         */
        private final transient String path;

        /**
         * Ctor.
         * @throws IOException If fails
         */
        public InFile() throws IOException {
            this(File.createTempFile("rultor", ".talk"));
            FileUtils.write(
                new File(this.path),
                String.format("<talk name='%s' number='1'/>", Talk.TEST_NAME),
                StandardCharsets.UTF_8
            );
        }

        /**
         * Ctor.
         * @param lines Lines to concat
         * @throws IOException If fails
         */
        public InFile(final String... lines) throws IOException {
            this(new XMLDocument(new Joined("", lines).toString()));
        }

        /**
         * Ctor.
         * @param xml XML to save
         * @throws IOException If fails
         */
        public InFile(final XML xml) throws IOException {
            this();
            FileUtils.write(
                new File(this.path),
                new StrictXML(xml, Talk.SCHEMA).toString(),
                StandardCharsets.UTF_8
            );
        }

        /**
         * Ctor.
         * @param file The file
         */
        public InFile(final File file) {
            this.path = file.getAbsolutePath();
        }

        @Override
        public Long number() throws IOException {
            return Long.parseLong(this.read().xpath("/talk/@number").get(0));
        }

        @Override
        public String name() throws IOException {
            return this.read().xpath("/talk/@name").get(0);
        }

        @Override
        public Date updated() {
            return new Date(new File(this.path).lastModified());
        }

        @Override
        public XML read() throws IOException {
            return Talk.UPGRADE.transform(
                new XMLDocument(
                    FileUtils.readFileToString(
                        new File(this.path), StandardCharsets.UTF_8
                    )
                )
            );
        }

        @Override
        public void modify(final Iterable<Directive> dirs) throws IOException {
            if (dirs.iterator().hasNext()) {
                final Node node = this.read().inner();
                try {
                    new Xembler(dirs).apply(node);
                } catch (final ImpossibleModificationException ex) {
                    throw new IllegalStateException(ex);
                }
                FileUtils.write(
                    new File(this.path),
                    new StrictXML(
                        new XMLDocument(node), Talk.SCHEMA
                    ).toString(),
                    StandardCharsets.UTF_8
                );
            }
        }

        @Override
        public void active(final boolean yes) {
            // nothing
        }
    }

}
