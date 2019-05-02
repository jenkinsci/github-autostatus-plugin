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
package org.jenkinsci.plugins.githubautostatus.notifiers;

import static org.junit.Assert.*;

import java.io.IOException;

import com.timgroup.statsd.NonBlockingStatsDClient;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.StatsdClient;
import org.jenkinsci.plugins.githubautostatus.StatsdWrapper;
import org.jenkinsci.plugins.githubautostatus.notifiers.StatsdNotifier;

/**
 *
 * @author shane.gearon@hootsuite.com
 */
public class StatsdNotifierTest {

    private StatsdNotifierConfig config;
    private StatsdNotifier notifier;
    private StatsdClient client;

    public StatsdNotifierTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        config = mock(StatsdNotifierConfig.class);
        when(config.getExternalizedID()).thenReturn("folder0 / folder1 /     folder.2/ folder  3#123");
        when(config.getStatsdHost()).thenReturn("test.valid.hostname");
        when(config.getStatsdPort()).thenReturn(8000);
        when(config.getStatsdBucket()).thenReturn("test.valid.bucket");
        client = mock(StatsdClient.class);
        notifier = new StatsdNotifier(client, config);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSanitizeBuildNumber() throws IOException {
        String out = notifier.sanitizeAll(config.getExternalizedID());
        // path sanitization should come first, leading and following whitespaces should
        // be turned into a single underscore.
        assertEquals("folder0_._folder1_._folder2._folder_3", out);
    }


    @Test
    public void testSanitizeAll() throws IOException {
        String out = notifier.sanitizeAll(config.getExternalizedID());
        // path sanitization should come first, leading and following whitespaces should
        // be turned into a single underscore.
        assertEquals("folder0_._folder1_._folder2._folder_3", out);
    }

    /**
     * Test valid endpoint enables config.
     */
    @Test
    public void testIsEnabled() throws Exception {
        StatsdNotifier instance = new StatsdNotifier(client, config);
        assertTrue(instance.isEnabled());
    }

    /**
     * Test that null client disables config.
     */
    @Test
    public void testIsDisabled() throws Exception {
        client = null;
        StatsdNotifier instance = new StatsdNotifier(client, config);
        assertFalse(instance.isEnabled());
    }

    /*
     * Test that branch path returns properly formatted string
     */
    @Test
    public void testGetBranchPath() throws Exception {
        when(config.getStatsdHost()).thenReturn("");
        when(config.getExternalizedID()).thenReturn("Main Folder/Sub Folder/job name/branch name");
        StatsdNotifier instance = new StatsdNotifier(config);
        assertEquals("pipeline.Main_Folder.Sub_Folder.job_name.branch_name", instance.getBranchPath());
    }

    /*
     * Test that build results send the correct stats
     */
    @Test
    public void testNotifyBuildState() throws Exception {
        when(config.getExternalizedID()).thenReturn("Main Folder/Sub Folder/job name/branch name");
        StatsdNotifier instance = new StatsdNotifier(client, config);
        instance.notifyBuildState("Job Name!", "Stage Name$", BuildState.CompletedSuccess);
        verify(client).increment("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.stage.Stage_Name.status.CompletedSuccess", 1);
        verify(client).time("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.stage.Stage_Name.duration", 0);
    }

    /*
     * Test that stage results send the correct stats
     */
    @Test
    public void testNotifyBuildStageStatus() throws Exception {
        int buildDuration = 600;
        when(config.getExternalizedID()).thenReturn("Main Folder/Sub Folder/job name/branch name");
        StatsdNotifier instance = new StatsdNotifier(client, config);
        instance.notifyBuildStageStatus("Job Name!", "Stage Name$", BuildState.CompletedError, buildDuration);
        verify(client).increment("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.stage.Stage_Name.status.CompletedError", 1);
        verify(client).time("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.stage.Stage_Name.duration", buildDuration);
    }

    /*
     * Test that stage results don't send while stage is Pending
     */
    @Test
    public void testNotifyBuildStageStatusPending() throws Exception {
        int buildDuration = 600;
        when(config.getExternalizedID()).thenReturn("Main Folder/Sub Folder/job name/branch name");
        StatsdNotifier instance = new StatsdNotifier(client, config);
        instance.notifyBuildStageStatus("Job Name!", "Stage Name$", BuildState.Pending, buildDuration);
        verify(client, times(0)).increment("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.job.status.Pending", 1);
        verify(client, times(0)).time("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.job.duration", buildDuration);
    }

    /*
     * Test that build results send the correct stats at the end of a build
     */
    @Test
    public void testNotifyFinalBuildStatus() throws Exception {
        int buildDuration = 600;
        int buildBlockedDuration = 55;
        when(config.getExternalizedID()).thenReturn("Main Folder/Sub Folder/job name/branch name");
        StatsdNotifier instance = new StatsdNotifier(client, config);
        instance.notifyFinalBuildStatus("Job Name!", BuildState.CompletedError, buildDuration, buildBlockedDuration);
        verify(client).increment("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.job.status.CompletedError", 1);
        verify(client).time("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.job.duration", buildDuration);
    }

    /*
     * Test that non stage errors log correct stats
     */
    @Test
    public void testSendNonStageError() throws Exception {
        when(config.getExternalizedID()).thenReturn("Main Folder/Sub Folder/job name/branch name");
        StatsdNotifier instance = new StatsdNotifier(client, config);
        instance.sendNonStageError("Job Name!", "Stage Name$");
        verify(client).increment("pipeline.Main_Folder.Sub_Folder.job_name.branch_name.stage.Stage_Name.non_stage_error", 1);
    }
}
