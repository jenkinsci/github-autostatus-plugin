/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
package org.jenkinsci.plugins.githubautostatus.integration;


import hudson.model.Result;
import jenkins.model.CauseOfInterruption;
import org.jenkinsci.plugins.githubautostatus.BuildStatusAction;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ScriptedPipelineTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    /**
     * Verifies a simple scripted pipeline that succeeds sends the correct notifications
     * @throws Exception
     */
    @Test
    public void testScriptedSuccess() throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "    stage('Stage 1') {\n" +
                        "        echo 'hi'\n" +
                        "    }\n" +
                        "    stage('Stage 2') {\n" +
                        "        echo 'bye'\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage 1"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage 2"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedSuccess), any());

        verify(buildStatus, times(2)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies a simple scripted pipeline that fails sends the correct notifications
     * @throws Exception
     */
    @Test
    public void testScriptedFail() throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "    stage('Stage 1') {\n" +
                        "        echo 'hi'\n" +
                        "    }\n" +
                        "    stage('Stage fail') {\n" +
                        "        error 'fail on purpose'\n" +
                        "    }\n" +
                        "    stage('Stage 2') {\n" +
                        "        echo 'bye'\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage 1"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage fail"), eq(BuildStage.State.CompletedError), anyLong());
        verify(buildStatus, times(0)).updateBuildStatusForStage(eq("Stage 2"), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedError), any());

        verify(buildStatus, times(2)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies an error that occurs outside of a stage is sent as an out of stage error
     * @throws Exception
     */
    @Test
    public void testScriptedOutOfStageError() throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "    stage('Stage 1') {\n" +
                        "        echo 'hi'\n" +
                        "    }\n" +
                        "    sh 'exit(1)'\n" +
                        "    stage('Stage 2') {\n" +
                        "        echo 'bye'\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage 1"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, atLeast(1)).sendNonStageError("script returned exit code 2");
        verify(buildStatus, times(0)).updateBuildStatusForStage(eq("Stage 2"), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedError), any());

        verify(buildStatus, times(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }
    /**
     * Verifies a labelled step isn't reported as a stage
     * @throws Exception
     */
    @Test
    public void testLabel() throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "    stage('The stage') {\n" +
                        "          sh(script: \"echo 'hello'\", label: 'echo')\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).addBuildStatus(any());

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("The stage"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }
}
