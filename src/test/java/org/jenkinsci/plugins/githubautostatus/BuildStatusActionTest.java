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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
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

    private MockedStatic<GithubNotificationConfig> githubNotificationConfigStatic;
    private MockedStatic<InfluxDbNotifierConfig> influxDbNotifierConfigStatic;
    private MockedStatic<StatsdNotifierConfig> statsdNotifierConfigStatic;
    private MockedStatic<StatsdClient> statsdClientStatic;
    private MockedStatic<HttpNotifierConfig> httpNotifierConfigStatic;

    @BeforeEach
    public void setUp() {
        repository = mock(GHRepository.class);
        when(repository.getName()).thenReturn(repoName);

        githubNotificationConfigStatic = mockStatic(GithubNotificationConfig.class);
        githubConfig = mock(GithubNotificationConfig.class);
        when(githubConfig.getRepo()).thenReturn(repository);
        when(githubConfig.getShaString()).thenReturn(sha);
        when(githubConfig.getBranchName()).thenReturn(branchName);
        githubNotificationConfigStatic.when(() -> GithubNotificationConfig.fromRun(any())).thenReturn(githubConfig);

        influxDbNotifierConfigStatic = mockStatic(InfluxDbNotifierConfig.class);
        influxDbNotifierConfigStatic
                .when(() -> InfluxDbNotifierConfig.fromGlobalConfig((String) isNull(), (String) isNull(), any()))
                .thenReturn(mock(InfluxDbNotifierConfig.class));

        statsdNotifierConfig = mock(StatsdNotifierConfig.class);
        statsdNotifierConfigStatic = mockStatic(StatsdNotifierConfig.class);
        statsdNotifierConfigStatic.when(() -> StatsdNotifierConfig.fromGlobalConfig(any())).thenReturn(statsdNotifierConfig);

        statsdClientStatic = mockStatic(StatsdClient.class);
        statsdClientStatic.when(() -> StatsdClient.getInstance(any(), any(), anyInt())).thenReturn(null);

        httpNotifierConfigStatic = mockStatic(HttpNotifierConfig.class);
        httpNotifierConfigStatic
                .when(() -> HttpNotifierConfig.fromGlobalConfig((String) isNull(), (String) isNull(), any()))
                .thenReturn(mock(HttpNotifierConfig.class));

        mockRun = mock(AbstractBuild.class);
        when(mockRun.getExternalizableId()).thenReturn(jobName);
    }

    @AfterEach
    public void tearDown() {
        if (httpNotifierConfigStatic != null) httpNotifierConfigStatic.close();
        if (statsdClientStatic != null) statsdClientStatic.close();
        if (statsdNotifierConfigStatic != null) statsdNotifierConfigStatic.close();
        if (influxDbNotifierConfigStatic != null) influxDbNotifierConfigStatic.close();
        if (githubNotificationConfigStatic != null) githubNotificationConfigStatic.close();
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

        assertFalse(instance.isIsDeclarativePipeline());
    }

    @Test
    public void testIsDeclarativePipelineTrue() throws IOException {
        BuildStatusAction instance = new BuildStatusAction(mockRun, targetUrl, new ArrayList<>());
        instance.setIsDeclarativePipeline(true);

        assertTrue(instance.isIsDeclarativePipeline());
    }
}
