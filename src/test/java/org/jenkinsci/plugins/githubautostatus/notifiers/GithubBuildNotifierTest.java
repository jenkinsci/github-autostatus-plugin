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
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.model.BuildState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
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

    public GithubBuildNotifierTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        repository = mock(GHRepository.class);
        when(repository.getName()).thenReturn(repoName);
    }

    @After
    public void tearDown() {
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
}
