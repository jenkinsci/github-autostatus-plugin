/*
 * The MIT License
 *
 * Copyright 2018 jxpearce.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.githubautostatus.config;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Run;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision;
import org.jenkinsci.plugins.githubautostatus.BuildStatusConfig;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.Proxy;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CredentialsMatchers.class, SCMRevision.class, PullRequestSCMRevision.class, BuildStatusConfig.class})
public class GithubNotificationConfigTest {

    @Mock
    private BuildStatusConfig config;

    public GithubNotificationConfigTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(BuildStatusConfig.class);
        when(config.getEnableGithub()).thenReturn(true);
        when(BuildStatusConfig.get()).thenReturn(config);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testConfigBranchSource() throws Exception {
        Run build = Mockito.mock(Run.class);
        SCMRevisionAction mockSCMRevisionAction = mock(SCMRevisionAction.class);
        when(build.getAction(SCMRevisionAction.class)).thenReturn(mockSCMRevisionAction);

        GitHubSCMSource source = mock(GitHubSCMSource.class);
        when(source.getCredentialsId()).thenReturn("git-user");
        when(source.getRepoOwner()).thenReturn("repo-owner");
        when(source.getRepository()).thenReturn("repo");
        BranchSCMHead head = new BranchSCMHead("test-branch");
        SCMRevisionImpl revision = new SCMRevisionImpl(head, "what-the-hash");
        when(mockSCMRevisionAction.getRevision()).thenReturn(revision);

        WorkflowMultiBranchProject mockProject = mock(WorkflowMultiBranchProject.class);
        WorkflowJob mockJob = new WorkflowJob(mockProject, "job-name");
        when(build.getParent()).thenReturn(mockJob);
        when(mockProject.getSCMSources()).thenReturn(Collections.singletonList(source));

        PowerMockito.mockStatic(CredentialsMatchers.class);
        when(CredentialsMatchers.firstOrNull(any(), any())).thenReturn(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "user-pass", null, "git-user", "git-password"));

        GitHub github = mock(GitHub.class);
        GHUser mockUser = mock(GHUser.class);
        GHRepository mockRepo = mock(GHRepository.class);
        when(github.getUser(any())).thenReturn(mockUser);
        when(mockUser.getRepository(any())).thenReturn(mockRepo);
        GitHubBuilder builder = PowerMockito.mock(GitHubBuilder.class);

        PowerMockito.when(builder.withProxy(Matchers.<Proxy>anyObject())).thenReturn(builder);
        PowerMockito.when(builder.withOAuthToken(anyString(), anyString())).thenReturn(builder);
        PowerMockito.when(builder.build()).thenReturn(github);
        PowerMockito.when(builder.withEndpoint(any())).thenReturn(builder);

        GithubNotificationConfig instance = GithubNotificationConfig.fromRun(build, builder);
        assertEquals("what-the-hash", instance.getShaString());
        assertEquals("test-branch", instance.getBranchName());
    }

    @Test
    public void testDisabledInConfig() {
        when(config.getEnableGithub()).thenReturn(false);
        assertNull(GithubNotificationConfig.fromRun(mock(Run.class), null));
    }

//    @Test
//    public void testConfigPullRequest() throws Exception {
//        Run build = Mockito.mock(Run.class);
//        SCMRevisionAction mockSCMRevisionAction = mock(SCMRevisionAction.class);
//        when(build.getAction(SCMRevisionAction.class)).thenReturn(mockSCMRevisionAction);
//
//        GitHubSCMSource source = mock(GitHubSCMSource.class);
//        when(source.getCredentialsId()).thenReturn("git-user");
//        when(source.getRepoOwner()).thenReturn("repo-owner");
//        when(source.getRepository()).thenReturn("repo");
//        PullRequestSCMHead head = mock(PullRequestSCMHead.class);
//        PullRequestSCMRevision revision = mock(PullRequestSCMRevision.class);
//
//        when(revision.getPullHash()).thenReturn("what-the-hash");
//        when(revision.getHead()).thenReturn(head);
//
//        when(mockSCMRevisionAction.getRevision()).thenReturn(revision);
//
//        GitHub github = mock(GitHub.class);
//        GHUser mockUser = mock(GHUser.class);
//        GHRepository mockRepo = mock(GHRepository.class);
//        when(github.getUser(any())).thenReturn(mockUser);
//        when(mockUser.getRepository(any())).thenReturn(mockRepo);
//        GitHubBuilder builder = PowerMockito.mock(GitHubBuilder.class);
//
//        PowerMockito.when(builder.withProxy(Matchers.<Proxy>anyObject())).thenReturn(builder);
//        PowerMockito.when(builder.withOAuthToken(anyString(), anyString())).thenReturn(builder);
//        PowerMockito.when(builder.build()).thenReturn(github);
//        PowerMockito.when(builder.withEndpoint(any())).thenReturn(builder);
//
//
//        GithubNotificationConfig instance = GithubNotificationConfig.fromRun(build, builder);
//    }
}
