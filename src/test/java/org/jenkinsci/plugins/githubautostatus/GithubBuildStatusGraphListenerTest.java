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
import hudson.model.Action;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.plugins.githubautostatus.notifiers.BuildState;
import org.jenkinsci.plugins.pipeline.StageStatus;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStages;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.verification.VerificationMode;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author jxpearce
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BuildStatusConfig.class, GithubNotificationConfig.class})
public class GithubBuildStatusGraphListenerTest {

    static String repoOwner = "repo-owner";
    static String repoName = "test-repo";
    static String branchName = "test-branch";
    static BuildStatusConfig config;

    public GithubBuildStatusGraphListenerTest() {
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
        config = mock(BuildStatusConfig.class);
        when(BuildStatusConfig.get()).thenReturn(config);

        GithubNotificationConfig githubConfig = mock(GithubNotificationConfig.class);
        PowerMockito.mockStatic(GithubNotificationConfig.class);
        when(GithubNotificationConfig.fromRun(any(), any())).thenReturn(githubConfig);
        when(githubConfig.getRepoOwner()).thenReturn(repoOwner);
        when(githubConfig.getRepoName()).thenReturn(repoName);
        when(githubConfig.getBranchName()).thenReturn(branchName);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testStageNode() throws IOException {
        StepStartNode stageNode = mock(StepStartNode.class);
        StageAction stageAction = mock(StageAction.class);
        FlowExecution execution = mock(FlowExecution.class);
        when(stageNode.getAction(StageAction.class)).thenReturn(stageAction);
        when(stageNode.getExecution()).thenReturn(execution);
        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        when(execution.getOwner()).thenReturn(owner);
        AbstractBuild build = mock(AbstractBuild.class);

//        WorkflowRun build = jenkins.createProject(WorkflowRun.class);
        when(owner.getExecutable()).thenReturn(build);
        ExecutionModelAction executionModel = mock(ExecutionModelAction.class);
        when(build.getAction(ExecutionModelAction.class)).thenReturn(executionModel);

        ModelASTStages stages = new ModelASTStages(null);
        when(executionModel.getStages()).thenReturn(stages);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageNode);
        verify(build).addAction(any(BuildStatusAction.class));
    }

    @Test
    public void testAtomNode() throws IOException {
//        StepAtomNode stageNode = mock(StepAtomNode.class);
//        StageAction stageAction = mock(StageAction.class);
        ErrorAction error = mock(ErrorAction.class);
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepAtomNode stageNode = new StepAtomNode(execution, null, mock(FlowNode.class));
        stageNode.addAction(error);
//        when(stageNode.getError()).thenReturn(error);
//        when(stageNode.getAction(StageAction.class)).thenReturn(stageAction);
//        when(stageNode.getExecution()).thenReturn(execution); 
        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        when(execution.getOwner()).thenReturn(owner);
        AbstractBuild build = mock(AbstractBuild.class);
        when(owner.getExecutable()).thenReturn(build);
//        ExecutionModelAction executionModel = mock(ExecutionModelAction.class);
        when(build.getAction(ExecutionModelAction.class)).thenReturn(null); // not declarative

//        ModelASTStages stages = new ModelASTStages(null);
//        when(executionModel.getStages()).thenReturn(stages);
        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageNode);
        verify(build).addAction(any(BuildStatusAction.class));
    }

    @Test
    public void testStepEndNode() throws Exception {
        // Mocked objects
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepStartNode stageStartNode = mock(StepStartNode.class);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, mock(FlowNode.class));
        ErrorAction error = mock(ErrorAction.class);
        stageEndNode.addAction(error);
        TimingAction startTime = mock(TimingAction.class);
        TimingAction endTime = mock(TimingAction.class);
        stageEndNode.addAction(endTime);
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        AbstractBuild build = mock(AbstractBuild.class);

        // get BuildStatusAction from StepEndNode
        when(execution.getOwner()).thenReturn(owner);
        when(owner.getExecutable()).thenReturn(build);
        when(build.getAction(BuildStatusAction.class)).thenReturn(buildStatus);

        // get StepStartNode from StepEndNode
        String startId = "15";
        // when(stageEndNode.getStartNode()).thenReturn(stageStartNode);
        when(stageStartNode.getId()).thenReturn(startId);
        when(execution.getNode(startId)).thenReturn(stageStartNode);

        // get time from StepStartNode to StepEndNode
        long time = 12345L;
        when(stageStartNode.getAction(TimingAction.class)).thenReturn(startTime);
        when(GithubBuildStatusGraphListener.getTime(stageStartNode, stageEndNode)).thenReturn(time);

        // get LabelAction from StepStartNode
        when(stageStartNode.getAction(LabelAction.class)).thenReturn(null);

        // get step name of StepStartNode
        when(stageStartNode.getStepName()).thenReturn(null);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageEndNode);
        verify(stageStartNode).getStepName();
    }

    @Test
    public void testBuildStateForStageWithError() throws IOException {
        StepStartNode stageStartNode = mock(StepStartNode.class);
        ErrorAction error = mock(ErrorAction.class);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        BuildState state = instance.buildStateForStage(stageStartNode, error);
        assertEquals(BuildState.CompletedError, state);
    }

    @Test
    public void testBuildStateForStageWithTag() throws IOException {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepStartNode stageStartNode = mock (StepStartNode.class);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, mock(FlowNode.class));
        TagsAction tag = mock(TagsAction.class);
        stageEndNode.addAction(tag);
        when(tag.getTagValue(StageStatus.TAG_NAME)).thenReturn("SKIPPED_FOR_FAILURE");

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        BuildState state = instance.buildStateForStage(stageEndNode, null);
        assertEquals(BuildState.SkippedFailure, state);
    }

    @Test
    public void testGetTime() throws IOException {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepStartNode stageStartNode = new StepStartNode(execution, null, mock(FlowNode.class));
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, mock(FlowNode.class));
        TimingAction startTime = mock(TimingAction.class);
        TimingAction endTime = mock(TimingAction.class);
        stageStartNode.addAction(startTime);
        stageEndNode.addAction(endTime);
        when(startTime.getStartTime()).thenReturn(1L);
        when(endTime.getStartTime()).thenReturn(5L);

        long time = GithubBuildStatusGraphListener.getTime(stageStartNode, stageEndNode);
        assertEquals(4L, time);
    }
//    /**
//     * Test of getTime method, of class GithubBuildStatusGraphListener.
//     */
//    @Test
//    public void testGetTime() {
//        System.out.println("getTime");
//        FlowNode startNode = null;
//        FlowNode endNode = null;
//        long expResult = 0L;
//        long result = GithubBuildStatusGraphListener.getTime(startNode, endNode);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
