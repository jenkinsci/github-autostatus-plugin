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

import org.jenkinsci.plugins.githubautostatus.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.InfluxDbNotifierConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.kohsuke.github.GHRepository;
import org.mockito.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author jxpearce
 */
public class BuildNotifierManagerTest {

    @Mock
    private GithubNotificationConfig githubNotificationConfig;
    @Mock
    private InfluxDbNotifierConfig influxDbNotificationConfig;
    @Mock
    private GHRepository repo;
    private BuildNotifierManager instance;
    private final String mockJobName = "mock-jobname";
    private final String mockTargetUrl = "mock-targeturl";
    private final String stageName = "mock-stagename";

    public BuildNotifierManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        instance = new BuildNotifierManager(mockJobName, mockTargetUrl);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInitialization() {
        assertEquals(mockJobName, instance.jobName);
        assertEquals(mockTargetUrl, instance.targetUrl);
    }

    /**
     * Verifies github notifier is added correctly.
     */
    @Test
    public void testAddGithubNotifier() {
        when(githubNotificationConfig.getRepo()).thenReturn(repo);
        BuildNotifier result = instance.addGithubNotifier(githubNotificationConfig);
        assertNotNull(result);

        assert (instance.notifiers.contains(result));
    }

    /**
     * Verifies influxdb notifier is not added when disabled.
     */
    @Test
    public void testAddInfluxDbNofifierDisabled() {
        when(influxDbNotificationConfig.getInfluxDbUrlString()).thenReturn("");
        BuildNotifier result = instance.addInfluxDbNotifier(influxDbNotificationConfig);
        assertNull(result);
    }

    /**
     * Verifies notifyBuildStageStatus calls notifiers
     */
    @Test
    public void testNotifyBuildStageStatus() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);
        instance.notifyBuildStageStatus(stageName, BuildState.CompletedSuccess, 0);

        verify(notifier).notifyBuildStageStatus(mockJobName, stageName, BuildState.CompletedSuccess, 0);
    }

    /**
     * Verifies notifyFinalBuildStatus calls notifiers
     */
    @Test
    public void testNotifyFinalBuildStatus() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);
        instance.notifyFinalBuildStatus(BuildState.CompletedSuccess, 0, 1);

        verify(notifier).notifyFinalBuildStatus(mockJobName, BuildState.CompletedSuccess, 0, 1);
    }

    /**
     * Verifies sendNonStageError calls notifiers
     */
    @Test
    public void testSendNonStageError() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);
        instance.sendNonStageError(stageName);

        verify(notifier).sendNonStageError(mockJobName, stageName);
    }

    /**
     * Verifies addBuildNotifier adds enabled notifiers
     */
    @Test
    public void testaddBuildNotifierEnabled() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        when(notifier.isEnabled()).thenReturn(true);
        instance.addBuildNotifier(notifier);

        assert (instance.notifiers.contains(notifier));
    }

    /**
     * Verifies addBuildNotifier does not add disabled notifiers
     */
    @Test
    public void testaddBuildNotifierDisabled() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        when(notifier.isEnabled()).thenReturn(false);
        instance.addBuildNotifier(notifier);

        assertFalse(instance.notifiers.contains(notifier));
    }
}
