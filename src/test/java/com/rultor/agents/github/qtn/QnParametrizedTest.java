/*
 * SPDX-FileCopyrightText: Copyright (c) 2009-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package com.rultor.agents.github.qtn;

import com.jcabi.github.Comment;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.jcabi.github.mock.MkGithub;
import com.jcabi.matchers.XhtmlMatchers;
import com.rultor.agents.github.Question;
import com.rultor.agents.github.Req;
import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * Tests for ${@link QnParametrized}.
 *
 * @since 1.3.6
 * @checkstyle MultipleStringLiteralsCheck (500 lines)
 */
final class QnParametrizedTest {

    /**
     * QnParametrized can fetch params.
     * @throws Exception In case of error.
     */
    @Test
    void fetchesParams() throws Exception {
        final Repo repo = new MkGithub().randomRepo();
        final Issue issue = repo.issues().create("", "");
        issue.comments().post(
            "hey, tag=`1.9` and server is `p5`, title is `Version 1.9.0`"
        );
        final Question origin = new Question() {
            @Override
            public Req understand(final Comment.Smart comment, final URI home) {
                return () -> new Directives()
                    .add("args").add("arg").set("hello, all").up().up()
                    .add("type").set("xxx").up();
            }
        };
        MatcherAssert.assertThat(
            "Parameters should be saved to the request",
            new Xembler(
                new Directives().add("request").append(
                    new QnParametrized(origin).understand(
                        new Comment.Smart(issue.comments().get(1)), new URI("#")
                    ).dirs()
                )
            ).xml(),
            XhtmlMatchers.hasXPaths(
                "/request[type='xxx']",
                "/request/args[count(arg) = 4]",
                "/request/args/arg[@name='tag' and .='1.9']",
                "/request/args/arg[@name='server' and .='p5']",
                "/request/args/arg[@name='title' and .='Version 1.9.0']"
            )
        );
    }

    /**
     * QnParametrized can ignore if there are no params.
     * @throws Exception In case of error.
     */
    @Test
    void ignoresEmptyParams() throws Exception {
        final Repo repo = new MkGithub().randomRepo();
        final Issue issue = repo.issues().create("", "");
        issue.comments().post("hey");
        MatcherAssert.assertThat(
            "No dirs should be added to the empty command",
            new QnParametrized(Question.EMPTY).understand(
                new Comment.Smart(issue.comments().get(1)), new URI("#1")
            ).dirs(),
            Matchers.emptyIterable()
        );
    }

    /**
     * QnParametrized can ignore empty request.
     * @throws Exception In case of error.
     */
    @Test
    void ignoresEmptyReq() throws Exception {
        final Repo repo = new MkGithub().randomRepo();
        final Issue issue = repo.issues().create("", "");
        issue.comments().post("hey you");
        MatcherAssert.assertThat(
            "Empty request should be in case of empty question",
            new QnParametrized(Question.EMPTY).understand(
                new Comment.Smart(issue.comments().get(1)), new URI("#2")
            ),
            Matchers.is(Req.EMPTY)
        );
    }

    /**
     * QnParametrized can ignore LATER request.
     * @throws Exception In case of error.
     */
    @Test
    void ignoresLaterReq() throws Exception {
        final Repo repo = new MkGithub().randomRepo();
        final Issue issue = repo.issues().create("", "");
        issue.comments().post("");
        final Question question = (comment, home) -> Req.LATER;
        MatcherAssert.assertThat(
            "Later request should be in the result",
            new QnParametrized(question).understand(
                new Comment.Smart(issue.comments().get(1)), new URI("#2")
            ),
            Matchers.is(Req.LATER)
        );
    }

}
