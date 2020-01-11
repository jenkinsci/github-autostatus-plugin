/*
 * The MIT License
 *
 * Copyright 2018 Jeff Pearce (GitHub jeffpearce).
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
import hudson.model.Queue.Executable;
import org.jenkinsci.plugins.githubautostatus.config.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.pipeline.StageStatus;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStages;
import org.jenkinsci.plugins.workflow.actions.*;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
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
        when(config.getEnableStatsd()).thenReturn(false);

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

        when(owner.getExecutable()).thenReturn(build);
        ExecutionModelAction executionModel = mock(ExecutionModelAction.class);
        when(build.getAction(ExecutionModelAction.class)).thenReturn(executionModel);

        ModelASTStages stages = new ModelASTStages(null);
        when(executionModel.getStages()).thenReturn(stages);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageNode);
        verify(build).addAction(any(BuildStatusAction.class));
    }

    public void testComplexPipeline() throws IOException {
        StepStartNode stageNode = mock(StepStartNode.class);
        StageAction stageAction = mock(StageAction.class);
        FlowExecution execution = mock(FlowExecution.class);
        when(stageNode.getAction(StageAction.class)).thenReturn(stageAction);
        when(stageNode.getExecution()).thenReturn(execution);
        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        when(execution.getOwner()).thenReturn(owner);
        AbstractBuild build = mock(AbstractBuild.class);

        when(owner.getExecutable()).thenReturn(build);
        ExecutionModelAction executionModel = mock(ExecutionModelAction.class);
        when(build.getAction(ExecutionModelAction.class)).thenReturn(executionModel);

        // Construct a complex pipeline model
        ModelASTStages stages = createStages("Outer Stage 1", "Outer Stage 2");
        ModelASTStages innerStages = createStages("Inner Stage 1", "Inner Stage 2", "Inner Stage 3");
        ModelASTStages innerInnerStages = createStages("Inner Inner Stage 1");
        ModelASTStages parallelStages = createStages("Parallel Stage 1", "Parallel Stage 2");
        stages.getStages().get(0).setStages(innerStages);
        innerStages.getStages().get(2).setStages(innerInnerStages);
        stages.getStages().get(1).setParallelContent(parallelStages.getStages());
        // Create a linear list of the pipeline stages for comparison
        List<String> fullStageList = Arrays.asList(new String[]{"Outer Stage 1", "Inner Stage 1", "Inner Stage 2", "Inner Stage 3", "Inner Inner Stage 1", "Outer Stage 2", "Parallel Stage 1", "Parallel Stage 2"});

        when(executionModel.getStages()).thenReturn(stages);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageNode);
        verify(build).addAction(any(BuildStatusAction.class));
        // Check that the pipeline stages found match the list of expected stages
        assertTrue(GithubBuildStatusGraphListener.getDeclarativeStages(build).equals(fullStageList));
    }

    @Test
    public void testAtomNode() throws IOException {
        ErrorAction error = mock(ErrorAction.class);
        CpsFlowExecution execution = mock(CpsFlowExecution.class);

        StepAtomNode stageNode = new StepAtomNode(execution, null, mock(FlowNode.class));
        stageNode.addAction(error);

        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        when(execution.getOwner()).thenReturn(owner);

        Executable exec = mock(Executable.class);
        when(owner.getExecutable()).thenReturn(exec);

        AbstractBuild build = mock(AbstractBuild.class);
        when(owner.getExecutable()).thenReturn(build);
        when(build.getAction(ExecutionModelAction.class)).thenReturn(null); // not declarative

        BuildStatusAction buildStatusAction = mock(BuildStatusAction.class);
        when(build.getAction(BuildStatusAction.class)).thenReturn(buildStatusAction);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageNode);
        verify(buildStatusAction).sendNonStageError(any());
    }

    @Test
    public void testStepEndNode() throws Exception {
        long time = 12345L;

        // Mocked objects
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepStartNode stageStartNode = mock(StepStartNode.class);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, mock(FlowNode.class));

        ErrorAction error = mock(ErrorAction.class);
        stageEndNode.addAction(error);

        TimingAction startTime = mock(TimingAction.class);
        TimingAction endTime = mock(TimingAction.class);
        stageEndNode.addAction(endTime);
        when(startTime.getStartTime()).thenReturn(0L);
        when(endTime.getStartTime()).thenReturn(time);

        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        AbstractBuild build = mock(AbstractBuild.class);

        // get BuildStatusAction from StepEndNode
        when(execution.getOwner()).thenReturn(owner);
        when(owner.getExecutable()).thenReturn(build);
        when(build.getAction(BuildStatusAction.class)).thenReturn(buildStatus);

        // get StepStartNode from StepEndNode
        String startId = "15";
        when(stageStartNode.getId()).thenReturn(startId);
        when(execution.getNode(startId)).thenReturn(stageStartNode);

        // get time from StepStartNode to StepEndNode
        when(stageStartNode.getAction(TimingAction.class)).thenReturn(startTime);
        LabelAction labelAction = new LabelAction("some label");
        when(stageStartNode.getAction(LabelAction.class)).thenReturn(labelAction);
        when(stageStartNode.getAction(StageAction.class)).thenReturn(mock(StageAction.class));

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageEndNode);
        verify(buildStatus).updateBuildStatusForStage(eq("some label"), eq(BuildStage.State.CompletedError), eq(time));
    }

    @Test
    public void testBuildStateForStageWithError() throws IOException {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepStartNode stageStartNode = mock(StepStartNode.class);
        ErrorAction error = mock(ErrorAction.class);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, mock(FlowNode.class));
        stageEndNode.addAction(error);

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        BuildStage.State state = instance.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.CompletedError, state);
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
        BuildStage.State state = instance.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.SkippedFailure, state);
    }

    /**
     * Verifies onNewHead adds the build action for an atom node if there's an error (used for sending errors that occur
     * out of a stage
     */
    @Test
    public void testAtomNodeAddsAction() throws IOException {
        ErrorAction error = mock(ErrorAction.class);
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        StepAtomNode stageNode = new StepAtomNode(execution, null, mock(FlowNode.class));
        stageNode.addAction(error);

        FlowExecutionOwner owner = mock(FlowExecutionOwner.class);
        when(execution.getOwner()).thenReturn(owner);
        AbstractBuild build = mock(AbstractBuild.class);
        when(owner.getExecutable()).thenReturn(build);
        when(build.getAction(ExecutionModelAction.class)).thenReturn(null); // not declarative

        GithubBuildStatusGraphListener instance = new GithubBuildStatusGraphListener();
        instance.onNewHead(stageNode);
        verify(build).addAction(any(BuildStatusAction.class));
    }

    private static ModelASTStages createStages(String... names) {
        ModelASTStages stages = new ModelASTStages(null);
        List<ModelASTStage> stageList = new ArrayList<ModelASTStage>();
        for (String name : names) {
            ModelASTStage stage = createStage(name);
            stageList.add(stage);
        }
        stages.setStages(stageList);
        return stages;
    }

    private static ModelASTStage createStage(String name) {
        ModelASTStage stage = new ModelASTStage(null);
        stage.setName(name);
        return stage;
    }

    private static List<String> getStageList(ModelASTStages stages) {
        List<String> stageList = new ArrayList<String>();
        for (ModelASTStage stage : stages.getStages()) {
            stageList.add(stage.getName());
        }
        return stageList;
    }

    @Test
    public void buildStateForStageSuccess() {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        when(execution.iotaStr()).thenReturn("1", "2", "3", "4");
        FlowStartNode parentNode = new FlowStartNode(execution, "5");
        StepStartNode stageStartNode = new StepStartNode(execution, null, parentNode);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, parentNode);

        BuildStage.State result = GithubBuildStatusGraphListener.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.CompletedSuccess, result);
    }

    @Test
    public void buildStateForStageError() {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        when(execution.iotaStr()).thenReturn("1", "2", "3", "4");
        FlowStartNode parentNode = new FlowStartNode(execution, "5");
        StepStartNode stageStartNode = new StepStartNode(execution, null, parentNode);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, parentNode);

        ErrorAction errorAction = mock(ErrorAction.class);
        stageEndNode.addAction(errorAction);

        BuildStage.State result = GithubBuildStatusGraphListener.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.CompletedError, result);
    }

    @Test
    public void buildStateForStageSkippedUnstable() {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        when(execution.iotaStr()).thenReturn("1", "2", "3", "4");
        FlowStartNode parentNode = new FlowStartNode(execution, "5");
        StepStartNode stageStartNode = new StepStartNode(execution, null, parentNode);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, parentNode);

        TagsAction tagsAction = mock(TagsAction.class);
        when(tagsAction.getTagValue(StageStatus.TAG_NAME)).thenReturn(StageStatus.getSkippedForUnstable());
        stageEndNode.addAction(tagsAction);

        BuildStage.State result = GithubBuildStatusGraphListener.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.SkippedUnstable, result);
    }

    @Test
    public void buildStateForStageSkippedConditional() {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        when(execution.iotaStr()).thenReturn("1", "2", "3", "4");
        FlowStartNode parentNode = new FlowStartNode(execution, "5");
        StepStartNode stageStartNode = new StepStartNode(execution, null, parentNode);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, parentNode);

        TagsAction tagsAction = mock(TagsAction.class);
        when(tagsAction.getTagValue(StageStatus.TAG_NAME)).thenReturn(StageStatus.getSkippedForConditional());
        stageEndNode.addAction(tagsAction);

        BuildStage.State result = GithubBuildStatusGraphListener.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.SkippedConditional, result);
    }

    @Test
    public void buildStateForStageFailedAndContinued() {
        CpsFlowExecution execution = mock(CpsFlowExecution.class);
        when(execution.iotaStr()).thenReturn("1", "2", "3", "4");
        FlowStartNode parentNode = new FlowStartNode(execution, "5");
        StepStartNode stageStartNode = new StepStartNode(execution, null, parentNode);
        StepEndNode stageEndNode = new StepEndNode(execution, stageStartNode, parentNode);

        TagsAction tagsAction = mock(TagsAction.class);
        when(tagsAction.getTagValue(StageStatus.TAG_NAME)).thenReturn(StageStatus.getFailedAndContinued());
        stageEndNode.addAction(tagsAction);

        BuildStage.State result = GithubBuildStatusGraphListener.buildStateForStage(stageStartNode, stageEndNode);
        assertEquals(BuildStage.State.CompletedError, result);
    }

    @Test
    public void testGetTime() {
        FlowNode startNode = mock(FlowNode.class);
        FlowNode endNode = mock(FlowNode.class);
        TimingAction startTime = mock(TimingAction.class);
        TimingAction endTime = mock(TimingAction.class);
        when(startNode.getAction(TimingAction.class)).thenReturn(startTime);
        when(endNode.getAction(TimingAction.class)).thenReturn(endTime);
        when(startTime.getStartTime()).thenReturn(1l);
        when(endTime.getStartTime()).thenReturn(3l);
        long result = GithubBuildStatusGraphListener.getTime(startNode, endNode);
        assertEquals(2, result);
    }

    @Test
    public void testGetTimeNoStartTime() {
        FlowNode startNode = mock(FlowNode.class);
        FlowNode endNode = mock(FlowNode.class);
        TimingAction endTime = mock(TimingAction.class);
        when(startNode.getAction(TimingAction.class)).thenReturn(null);
        when(endNode.getAction(TimingAction.class)).thenReturn(endTime);
        when(endTime.getStartTime()).thenReturn(3l);
        long result = GithubBuildStatusGraphListener.getTime(startNode, endNode);
        assertEquals(0, result);
    }

    @Test
    public void testGetTimeNoEndTime() {
        FlowNode startNode = mock(FlowNode.class);
        FlowNode endNode = mock(FlowNode.class);
        TimingAction startTime = mock(TimingAction.class);
        when(startNode.getAction(TimingAction.class)).thenReturn(startTime);
        when(endNode.getAction(TimingAction.class)).thenReturn(null);
        when(startTime.getStartTime()).thenReturn(1l);
        long result = GithubBuildStatusGraphListener.getTime(startNode, endNode);
        assertEquals(0, result);
    }
}
