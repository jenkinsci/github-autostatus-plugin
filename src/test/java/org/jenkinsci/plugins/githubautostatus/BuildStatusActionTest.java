/*
 * The MIT License
 *
 * Copyright 2017 jxpearce.
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
package org.jenkinsci.plugins.githubautostatus;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.jenkinsci.plugins.githubautostatus.config.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.config.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.config.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GithubNotificationConfig.class, BuildNotifierManager.class, InfluxDbNotifierConfig.class, StatsdNotifierConfig.class, HttpNotifierConfig.class, StatsdClient.class})
public class BuildStatusActionTest {

    static String jobName = "mock-job";
    static String stageName = "Stage 1";
    static String repoName = "mock-repo";
    static String branchName = "mock-branch";
    static String sha = "mock-sha";
    static String targetUrl = "http://mock-target";
    static GHRepository repository;
    static GithubNotificationConfig githubConfig;
    static StatsdNotifierConfig statsdNotifierConfig;
    static BuildNotifierManager buildNotifierManager;
    Run<?,?> mockRun;

    public BuildStatusActionTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));

        repository = mock(GHRepository.class);
        when(repository.getName()).thenReturn(repoName);

        PowerMockito.mockStatic(GithubNotificationConfig.class);
        githubConfig = mock(GithubNotificationConfig.class);
        when(githubConfig.getRepo()).thenReturn(repository);
        when(githubConfig.getShaString()).thenReturn(sha);
        when(githubConfig.getBranchName()).thenReturn(branchName);
        when(GithubNotificationConfig.fromRun(anyObject())).thenReturn(githubConfig);

        PowerMockito.mockStatic(InfluxDbNotifierConfig.class);
        when(InfluxDbNotifierConfig.fromGlobalConfig((String)isNull(), (String)isNull(), any())).thenReturn(mock(InfluxDbNotifierConfig.class));

        statsdNotifierConfig = mock(StatsdNotifierConfig.class);
        PowerMockito.mockStatic(StatsdNotifierConfig.class);
        when(StatsdNotifierConfig.fromGlobalConfig(any())).thenReturn(statsdNotifierConfig);

        PowerMockito.mockStatic(StatsdClient.class);
        when(StatsdClient.getInstance(any(), any(), anyInt())).thenReturn(null);

        PowerMockito.mockStatic(HttpNotifierConfig.class);
        when(HttpNotifierConfig.fromGlobalConfig((String)isNull(), (String)isNull(), any())).thenReturn(mock(HttpNotifierConfig.class));

        mockRun = mock(AbstractBuild.class);
        when(mockRun.getExternalizableId()).thenReturn(jobName);
    }


    /**
     * Verifies status is sent for initial stages when notifier is added
     *
     * @throws java.io.IOException
     */
    @Test
    public void testInitialStage() throws IOException {
        List<BuildStage> model = new ArrayList<BuildStage>();
        model.add(new BuildStage(stageName));
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, model);

        verify(repository).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
    }

    /**
     * Verifies addBuildStatus calls createCommitStatus with a status of pending
     *
     * @throws java.io.IOException
     */
    @Test
    public void testAddBuildStatusGitHub() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.addBuildStatus(stageName);

        verify(repository).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
    }

    /**
     * Verifies updating a stage with success sends the correct status
     *
     * @throws java.io.IOException
     */
    @Test
    public void testStageSuccessGitHub() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.addBuildStatus(stageName);

        instance.updateBuildStatusForStage(stageName, BuildStage.State.CompletedSuccess);

        verify(repository).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
        verify(repository).createCommitStatus(sha, GHCommitState.SUCCESS, targetUrl, "Stage built successfully", stageName);
    }

    /**
     * Verifies updating a stage with an error sends the correct status
     *
     * @throws java.io.IOException
     */
    @Test
    public void testStageErrorGitHub() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.addBuildStatus(stageName);

        instance.updateBuildStatusForStage(stageName, BuildStage.State.CompletedError);

        verify(repository).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
        verify(repository).createCommitStatus(sha, GHCommitState.ERROR, targetUrl, "Failed to build stage", stageName);
    }

    /**
     * Verifies attempt to send status for invalid stage is ignored
     *
     * @throws java.io.IOException
     */
    @Test
    public void testIgnoreInvalidStageGitHub() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());

        instance.updateBuildStatusForStage(stageName, BuildStage.State.CompletedSuccess);

        verify(repository, never()).createCommitStatus(any(), any(), any(), any());
    }

    /**
     * Verifies pending status recorded before the notifier was added are sent
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSendUnsentPendingStages() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.addBuildStatus(stageName);

        verify(repository).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
    }

    /**
     * Verifies close sends updated status information for "pending" stages
     *
     * @throws java.io.IOException
     */
    @Test
    public void testCloseUpdatesPendingStatuses() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.addBuildStatus(stageName);

        instance.close();

        verify(repository).createCommitStatus(sha, GHCommitState.SUCCESS, targetUrl, "Stage built successfully", stageName);
    }

    @Test
    public void testIsDeclarativePipelineFalse() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.setIsDeclarativePipeline(false);
        
        assumeFalse(instance.isIsDeclarativePipeline());
    }

    @Test
    public void testIsDeclarativePipelineTrue() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.setIsDeclarativePipeline(true);
        
        assumeTrue(instance.isIsDeclarativePipeline());
    }
}
