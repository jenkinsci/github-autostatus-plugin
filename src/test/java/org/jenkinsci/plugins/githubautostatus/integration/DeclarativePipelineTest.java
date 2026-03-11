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
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WithJenkins
public class DeclarativePipelineTest {

    /**
     * Verifies a simple pipeline that succeeds sends the correct notifications
     * @throws Exception
     */
    @Test
    public void testSuccess(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    stages {\n" +
                        "        stage('The stage') {\n" +
                        "            steps {\n" +
                        "                echo 'hello'\n" +
                        "                echo 'goodbye'\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("The stage"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedSuccess), any());

        verify(buildStatus, times(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies notifications are sent for nested stages
     * @throws Exception
     */
    @Test
    public void testNested(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "agent any\n" +
                        "stages {\n" +
                            "stage('Parallel') {\n" +
                                "parallel {\n" +
                                    "stage ('Stage A') {\n" +
                                        "steps {\n" +
                                            "sh 'echo hello'\n" +
                                        "}\n" +
                                    "}\n" +
                                    "stage ('Stage B') {\n" +
                                        "steps {\n" +
                                            "sh 'echo hello'\n" +
                                        "}\n" +
                                    "}\n" +
                                "}\n" +
                            "}\n" +
                        "}\n" +
                    "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Parallel"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage A"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Stage B"), eq(BuildStage.State.CompletedSuccess), anyLong());

        verify(buildStatus, times(3)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies a simple pipeline that fails sends the correct notifications
     * @throws Exception
     */
    @Test
    public void testFail(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    stages {\n" +
                        "        stage('The stage') {\n" +
                        "            steps {\n" +
                        "                echo 'hello'\n" +
                        "                error('fail')\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("The stage"), eq(BuildStage.State.CompletedError), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedError), any());

        verify(buildStatus, atMost(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, atMost(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies stages with caught errors are reported as success
     * @throws Exception
     */
    @Test
    public void testCaughtException(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    stages {\n" +
                        "        stage('The stage') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    try {\n" +
                        "                        sh 'exit 1'\n" +
                        "                    } catch (Exception ignored) {}\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("The stage"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedSuccess), any());

        verify(buildStatus, times(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }
    /**
     * Verifies stage status can be reported correctly when set to FAILED in catchError
     * @throws Exception
     */
    @Test
    public void testCaughtExceptionSetStageFail(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    stages {\n" +
                        "        stage('Before stage') {\n" +
                        "            steps {\n" +
                        "                echo 'hello'\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Error stage') {\n" +
                        "            steps {\n" +
                        "                echo 'before step'\n" +
                        "                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {\n" +
                        "                    error 'fail on purpose'\n" +
                        "                }\n" +
                        "                echo 'after step'\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('After stage') {\n" +
                        "            steps {\n" +
                        "                echo 'goodbye'\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Before stage"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Error stage"), eq(BuildStage.State.CompletedError), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("After stage"), eq(BuildStage.State.CompletedSuccess), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedSuccess), any());

        verify(buildStatus, times(3)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies stage status can be reported correctly when set to ABORTED in catchError
     * @throws Exception
     */
    @Test
    public void testCaughtExceptionSetStageAbort(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    stages {\n" +
                        "        stage('Error stage') {\n" +
                        "            steps {\n" +
                        "                catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {\n" +
                        "                    error 'fail on purpose'\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);
        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Error stage"), eq(BuildStage.State.Aborted), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.CompletedSuccess), any());

        verify(buildStatus, times(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }

    /**
     * Verifies an aborted stage is reported correctly
     * @throws Exception
     */
    @Test
    public void testAbort(JenkinsRule r) throws Exception {

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    stages {\n" +
                        "        stage('Error stage') {\n" +
                        "            steps {\n" +
                        "                sleep time: 5, unit: 'MINUTES'\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                true));

        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        BuildStatusAction buildStatus = mock(BuildStatusAction.class);
        b.addOrReplaceAction(buildStatus);

        WorkflowRun runningBuild = null;
        while (runningBuild == null) {
            runningBuild = p.getBuildByNumber(1);
            Thread.sleep(10);
        }
        Thread.sleep(5000);
        runningBuild.getExecution().interrupt(Result.ABORTED, new CauseOfInterruption.UserInterruption("xxxx"));

        r.waitForCompletion(b);
        Thread.sleep(500);

        verify(buildStatus, times(1)).updateBuildStatusForStage(eq("Error stage"), eq(BuildStage.State.Aborted), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(eq(BuildStage.State.Aborted), any());

        verify(buildStatus, times(1)).updateBuildStatusForStage(any(), any(), anyLong());
        verify(buildStatus, times(1)).updateBuildStatusForJob(any(), any());
    }
}
