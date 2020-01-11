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
package org.jenkinsci.plugins.githubautostatus;

import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.TaskListener;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.model.BuildState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildStatusJobListenerTest {

    public BuildStatusJobListenerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Verifies BuildStatusJobListener onCompleted can be called when there's no
     * build action
     */
    @Test
    public void testOnCompletedNoBuildAction() {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getAction(BuildStatusAction.class)).thenReturn(null);
        Job job = mock(Job.class);
        when(build.getParent()).thenReturn(job);
        TaskListener listener = null;

        BuildStatusJobListener instance = new BuildStatusJobListener();

        instance.onCompleted(build, listener);
    }

    @Test
    public void testOnCompletedSuccess() {
        AbstractBuild build = mock(AbstractBuild.class);
        BuildStatusAction action = mock(BuildStatusAction.class);
        when(build.getResult()).thenReturn(Result.SUCCESS);
        when(build.getAction(BuildStatusAction.class)).thenReturn(action);
        Job job = mock(Job.class);
        when(build.getParent()).thenReturn(job);
        TaskListener listener = null;

        BuildStatusJobListener instance = new BuildStatusJobListener();
        HashMap<String, Object> jobParams = new HashMap<String, Object>();

        instance.onCompleted(build, listener);
        verify(action).updateBuildStatusForJob(eq(BuildStage.State.CompletedSuccess), anyMapOf(String.class, Object.class));
    }

    @Test
    public void testOnCompletedFailure() {
        AbstractBuild build = mock(AbstractBuild.class);
        BuildStatusAction action = mock(BuildStatusAction.class);
        when(build.getResult()).thenReturn(Result.FAILURE);
        when(build.getAction(BuildStatusAction.class)).thenReturn(action);
        Job job = mock(Job.class);
        when(build.getParent()).thenReturn(job);
        TaskListener listener = null;

        BuildStatusJobListener instance = new BuildStatusJobListener();

        instance.onCompleted(build, listener);
        verify(action).updateBuildStatusForJob(eq(BuildStage.State.CompletedError), anyMapOf(String.class, Object.class));
    }
}
