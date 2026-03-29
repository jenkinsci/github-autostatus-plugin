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
package org.jenkinsci.plugins.githubautostatus.notifiers;

import java.io.IOException;
import java.util.Collections;
import org.jenkinsci.plugins.githubautostatus.config.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.HttpException;
import static org.mockito.Mockito.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class GithubBuildNotifierTest {

    static String jobName = "mock-job";
    static String stageName = "Stage 1";
    static String repoName = "mock-repo";
    static String branchName = "mock-branch";
    static String sha = "mock-sha";
    static String targetUrl = "http://mock-target";
    static GHRepository repository;

    @BeforeEach
    public void setUp() {
        repository = mock(GHRepository.class);
        when(repository.getName()).thenReturn(repoName);
    }

    /**
     * Verifies notifier is disabled if there's no repo
     */
    @Test
    public void testDisabled() {
        GithubBuildNotifier notifier = new GithubBuildNotifier(null, "", "");

        assertFalse(notifier.isEnabled());
    }

    /**
     * Verifies notifier is disabled if there's a repo
     */
    @Test
    public void testEnabled() {
        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, "", "");

        assertTrue(notifier.isEnabled());
    }

    /**
     * Verifies notifier sends pending status
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSendPending() throws IOException {
        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl);

        BuildStage stageItem = new BuildStage(stageName);

        notifier.notifyBuildStageStatus(jobName, stageItem);
        verify(repository).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
    }

    /**
     * Verifies notifier sends success status
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSendSuccess() throws IOException {
        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl);

        BuildStage stageItem = new BuildStage(stageName);
        stageItem.setBuildState(BuildStage.State.CompletedSuccess);

        notifier.notifyBuildStageStatus(jobName, stageItem);
        verify(repository).createCommitStatus(sha, GHCommitState.SUCCESS, targetUrl, "Stage built successfully", stageName);
    }

    /**
     * Verifies notifier sends error status
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSendError() throws IOException {
        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl);

        BuildStage stageItem = new BuildStage(stageName);
        stageItem.setBuildState(BuildStage.State.CompletedError);

        notifier.notifyBuildStageStatus(jobName, stageItem);
        verify(repository).createCommitStatus(sha, GHCommitState.ERROR, targetUrl, "Failed to build stage", stageName);
    }

    /**
     * Verifies notifyFinalBuildStatus doesn't send any commit status
     * @throws IOException 
     */
    @Test
    public void testNotifyFinalBuildStatus() throws IOException {
        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl);
        notifier.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, Collections.emptyMap());
        verify(repository, never()).createCommitStatus(any(), any(), any(), any());
    }
    
    /**
     * Verifies notifier doesn't report errors that happen outside a stage.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testNonStageError() throws IOException {
        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl);

        BuildStage stageItem = new BuildStage(stageName);
        stageItem.setIsStage(false);
        stageItem.setBuildState(BuildStage.State.CompletedError);
        
        notifier.notifyBuildStageStatus(jobName, stageItem);

        verify(repository, never()).createCommitStatus(any(), any(), any(), any());
    }

    /**
     * Verifies that on HTTP 401 (token expired), the notifier refreshes
     * credentials and retries the API call when a config is provided.
     */
    @Test
    public void testRetryOn401WithConfig() throws IOException {
        GHRepository freshRepo = mock(GHRepository.class);
        GithubNotificationConfig config = mock(GithubNotificationConfig.class);
        when(config.createRepository()).thenReturn(freshRepo);

        // First call throws 401, simulating expired GitHub App token
        doThrow(new HttpException("Unauthorized", 401, "Unauthorized", null))
                .when(repository).createCommitStatus(any(), any(), any(), any(), any());

        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl, config);

        BuildStage stageItem = new BuildStage(stageName);
        notifier.notifyBuildStageStatus(jobName, stageItem);

        // Verify credentials were refreshed
        verify(config).createRepository();
        // Verify retry was attempted on the fresh repo
        verify(freshRepo).createCommitStatus(sha, GHCommitState.PENDING, targetUrl, "Building stage", stageName);
    }

    /**
     * Verifies that on HTTP 401 without a config, no retry is attempted
     * (backward-compatible behavior).
     */
    @Test
    public void testNoRetryOn401WithoutConfig() throws IOException {
        doThrow(new HttpException("Unauthorized", 401, "Unauthorized", null))
                .when(repository).createCommitStatus(any(), any(), any(), any(), any());

        GithubBuildNotifier notifier = new GithubBuildNotifier(repository, sha, targetUrl);

        BuildStage stageItem = new BuildStage(stageName);
        notifier.notifyBuildStageStatus(jobName, stageItem);

        // Only one call attempt, no retry
        verify(repository, times(1)).createCommitStatus(any(), any(), any(), any(), any());
    }
}
