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

import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.config.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.config.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.config.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.model.BuildState;
import org.junit.*;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildNotifierManagerTest {

    @Mock
    private GithubNotificationConfig githubNotificationConfig;
    @Mock
    private InfluxDbNotifierConfig influxDbNotificationConfig;
    @Mock
    private StatsdNotifierConfig statsdNotificationConfig;
    @Mock
    private HttpNotifierConfig httpNotifierConfig;
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
        instance = BuildNotifierManager.newInstance(mockJobName, mockTargetUrl);
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
     * Verifies GitHub notifier is added correctly.
     */
    @Test
    public void testAddGithubNotifier() {
        when(githubNotificationConfig.getRepo()).thenReturn(repo);
        BuildNotifier result = instance.addGithubNotifier(githubNotificationConfig);
        assertNotNull(result);

        assert (instance.notifiers.contains(result));
    }

    /**
     * Verifies InfluxDB notifier is not added when disabled.
     */
    @Test
    public void testAddInfluxDbNotifierDisabled() {
        when(influxDbNotificationConfig.getInfluxDbUrlString()).thenReturn("");
        BuildNotifier result = instance.addInfluxDbNotifier(influxDbNotificationConfig);
        assertNull(result);
    }

    /**
     * Verifies HTTP notifier is not added when enabled.
     */
    @Test
    public void testAddHttpNotifier() {
        when(httpNotifierConfig.getHttpEndpoint()).thenReturn("http://example.com");
        BuildNotifier result = instance.addHttpNotifier(httpNotifierConfig);
        assertNotNull(result);
        assertTrue(instance.notifiers.contains(result));
    }

    /**
     * Verifies HTTP notifier is not added when disabled.
     */
    @Test
    public void testAddHttpNotifierDisabled() {
        when(httpNotifierConfig.getHttpEndpoint()).thenReturn("");
        BuildNotifier result = instance.addHttpNotifier(httpNotifierConfig);
        assertNull(result);
    }

    /**
     * Verifies notifyBuildStageStatus calls notifiers
     */
    @Test
    public void testNotifyBuildStageStatus() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);
        
        BuildStage stageItem = new BuildStage(stageName);
        stageItem.setBuildState(BuildStage.State.CompletedSuccess);
        
        instance.notifyBuildStageStatus(stageItem);

        verify(notifier).notifyBuildStageStatus(mockJobName, stageItem);
    }

    /**
     * Verifies notifyFinalBuildStatus calls notifiers
     */
    @Test
    public void testNotifyFinalBuildStatus() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);

        instance.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, Collections.emptyMap());

        verify(notifier).notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, Collections.emptyMap());
    }

    /**
     * Verifies sendNonStageError calls notifiers that want them
     */
    @Test
    public void testSendNonStageErrorWantsTrue() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);
        when(notifier.wantsOutOfStageErrors()).thenReturn(true);

        BuildStage stageItem = new BuildStage(stageName,
                new HashMap<>(),
                BuildStage.State.CompletedError);
        stageItem.setIsStage(false);


        instance.sendNonStageError(stageItem);

        verify(notifier).notifyBuildStageStatus(eq(mockJobName), any(BuildStage.class));
    }

    /**
     * Verifies sendNonStageError doeesn't call notifiers that don' want them
     */
    @Test
    public void testSendNonStageErrorWantsFalse() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        instance.notifiers.add(notifier);
        when(notifier.wantsOutOfStageErrors()).thenReturn(false);

        BuildStage stageItem = new BuildStage(stageName,
                new HashMap<>(),
                BuildStage.State.CompletedError);
        stageItem.setIsStage(false);


        instance.sendNonStageError(stageItem);

        verify(notifier, never()).notifyBuildStageStatus(any(), any());
    }

    /**
     * Verifies addBuildNotifier adds enabled notifiers
     */
    @Test
    public void testAddBuildNotifierEnabled() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        when(notifier.isEnabled()).thenReturn(true);
        instance.addBuildNotifier(notifier);

        assert (instance.notifiers.contains(notifier));
    }

    /**
     * Verifies addBuildNotifier does not add disabled notifiers
     */
    @Test
    public void testAddBuildNotifierDisabled() {
        GithubBuildNotifier notifier = mock(GithubBuildNotifier.class);
        when(notifier.isEnabled()).thenReturn(false);
        instance.addBuildNotifier(notifier);

        assertFalse(instance.notifiers.contains(notifier));
    }
}
