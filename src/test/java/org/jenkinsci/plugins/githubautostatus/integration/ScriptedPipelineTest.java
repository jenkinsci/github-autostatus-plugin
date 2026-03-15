/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
package org.jenkinsci.plugins.githubautostatus.integration;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.githubautostatus.BuildStatusAction;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import org.junit.jupiter.api.Timeout;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.stream.SystemErr;
import uk.org.webcompere.systemstubs.stream.output.MultiplexOutput;
import uk.org.webcompere.systemstubs.stream.output.Output;
import uk.org.webcompere.systemstubs.stream.output.TapStream;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WithJenkins
public class ScriptedPipelineTest {

    private static final Logger LOGGER = Logger.getLogger(ScriptedPipelineTest.class.getName());

    // Prepare to capture CME clues in JVM or Jenkins instance
    // logs (stderr -- sometimes the problem is reported there,
    // but does not cause a crash for any of the runs). This
    // apparently must be a class-wide rule (so it can tap into
    // the stream before Jenkins starts). For more details,
    // please see:
    // * https://github.com/webcompere/system-stubs
    // * https://www.baeldung.com/java-system-stubs
    @SystemStub
    private SystemErr systemErrTap = new SystemErr(new MultiplexOutput(new TapStream(), Output.fromStream(System.err)));

    /**
     * Verifies a simple scripted pipeline that succeeds sends the correct notifications
     * @throws Exception
     */
    @Test
    public void testScriptedSuccess(JenkinsRule r) throws Exception {

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
    public void testScriptedFail(JenkinsRule r) throws Exception {

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
    public void testScriptedOutOfStageError(JenkinsRule r) throws Exception {

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
    public void testLabel(JenkinsRule r) throws Exception {

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

    /**
     * Various events in Jenkins can cause saving of Workflow state,
     * even if particular jobs are configured for "performance" mode
     * to avoid causing this themselves in transitions between steps.
     * This causes an XStream export of Java objects, which may crash
     * with a {@link ConcurrentModificationException} if certain
     * complex properties of Jenkins Actions are being modified at
     * the same time (e.g. map of files/checksums is updated here):
     * <pre>
     * java.util.ConcurrentModificationException
     * ...
     * Caused: java.lang.RuntimeException: Failed to serialize
     *      net.masterthought.jenkins.SafeArchiveServingAction#fileChecksums
     *      for class net.masterthought.jenkins.SafeArchiveServingRunAction
     * ...
     * Caused: java.lang.RuntimeException: Failed to serialize hudson.model.Actionable#actions
     *      for class org.jenkinsci.plugins.workflow.job.WorkflowRun
     * ...
     * </pre>
     *
     * This test aims to reproduce the issue, and eventually confirm
     * a fix and non-regression.<br/>
     *
     * Alas, "reliably catching a non-deterministic race condition"
     * is maybe an oxymoron in itself, so we try to do our best here.<br/>
     *
     * Initially modeled after test for this JENKINS-76294 epic made
     * in Lockable Resources and Cucumber Reports plugins.<br/>
     *
     * @throws Exception  If test failed
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    @Issue("JENKINS-76294")
    public void testNoCmeWhileSavingXStreamVsBuildStatusAction(JenkinsRule r) throws Exception {
        // How many parallel stages would we use before saving WorkflowRun
        // state inside the pipeline run, and overall?
        int preflood = 25;
        int maxflood = 75;

        // More workers to increase the chaos in competition for resources;
        // this number should exceed maxRuns (agents are dedicated to a job
        // so they can all run simultaneously and not wait for executors).
        int extraAgents = 16;

        // How many jobs run in parallel?
        // Note that along with the amount of agents and maxflood
        // this dictates how long the test runs.
        int maxRuns = 3;
        List<WorkflowJob> wfJobs = new ArrayList<>();
        List<WorkflowRun> wfRuns = new ArrayList<>();

        // Substrings we would seek in logs to say the problem happened:
        List<String> indicatorsCME = new ArrayList<>();
        indicatorsCME.add("Failed to serialize");
        indicatorsCME.add("java.util.ConcurrentModificationException");

        // Prepare to capture CME clues in JVM or Jenkins instance logs
        // (sometimes the problem is reported there, but does not cause
        // a crash for any of the runs).
        Logger capturingLogger = Logger.getLogger(""); // root, or specific package/logger
        StringBuilder capturedLogs = new StringBuilder();
        Handler capturingLogHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                capturedLogs
                        .append(record.getLevel())
                        .append(": ")
                        .append(record.getMessage())
                        .append('\n');
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };

        // Involve also the lag and race of remote executions
        LOGGER.info("create extra build agents");
        for (int i = 1; i <= extraAgents; i++) {
            try {
                // Match JOB_NAME like "test1"
                r.createSlave("ExtraAgent_" + i, "worker-test" + (i % maxRuns), null);
            } catch (Exception error) {
                LOGGER.warning(error.toString());
            }
        }
        LOGGER.info("create extra build agents done");

        LOGGER.info("define " + maxRuns + " test workflows");
        String pipeCode = "import java.lang.Math;\n"
                + "import java.util.Random;\n"
                +
                // Do not occupy all readers at first, so all our
                // jobs can get defined and started simultaneously
                // (avoid them "waiting for available executors")
                "sleep 3\n"
                + "def parstages = [:]\n"
                + "def repoDir = ''\n"
                + "node(label: 'worker-' + env.JOB_NAME) {\n"
                + "  dir('repo') {\n"
                + "    sh ''' git init . && echo 'test' > test && git add test && git config --local user.name 'Jenkins Test' && git config --local user.email 'JenkinsTest@example.org' && git commit -m 'initial commit' '''\n"
                + "    repoDir = pwd()\n"
                + "  }\n"
                + "}\n"
                +
                // flood with cucumber actions, including logging about them
                "def preflood = " + preflood + "\n"
                + "def maxflood = " + maxflood + "\n"
                + "for (int i = 1; i < preflood; i++) {\n"
                +
                // Note that we must use toString() and explicit vars to avoid
                // seeing the same values at the time of GString evaluation
                "  String iStr = String.valueOf(i)\n"
                + "  parstages[\"stage-${iStr}\".toString()] = {\n"
                + "    node(label: 'worker-' + env.JOB_NAME) {\n"
                + "      sleep 1\n"
                + "      dir(\"subdir-${iStr}\".toString()) {\n"
                + "        org.jenkinsci.plugins.githubautostatus.BuildStatusAction buildStatusAction = currentBuild.rawBuild.getAction(org.jenkinsci.plugins.githubautostatus.BuildStatusAction.class)\n"
                + "        if (buildStatusAction != null) {\n"
                + "          buildStatusAction.addBuildStatus(\"stage-${iStr}\".toString())\n"
                + "          buildStatusAction.updateBuildStatusForStage(\"stage-${iStr}\".toString(), org.jenkinsci.plugins.githubautostatus.model.BuildStage.State.CompletedSuccess)\n"
                + "        }\n"
                + "        checkout scmGit(userRemoteConfigs: [[url: 'file://' + repoDir]])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                +
                // force a save while mutations are happening
                "parstages['saver'] = {\n"
                + "  org.jenkinsci.plugins.workflow.job.WorkflowRun r = currentBuild.rawBuild\n"
                + "  r.save()\n"
                + "}\n"
                +
                // sandwiching makes it more likely to get the race condition
                // as someone works with files while XStream kicks in
                "for (int i = preflood; i < maxflood; i++) {\n"
                + "  String iStr = String.valueOf(i)\n"
                + "  String iStrName = \"Build ${JOB_NAME} #${BUILD_ID} in parstage ${iStr}\".toString()\n"
                + "  parstages[\"stage-${iStr}\".toString()] = {\n"
                + "    node(label: 'worker-' + env.JOB_NAME) {\n"
                +
                // Changes of currentBuild should cause some saves too
                // (also badges, SCM steps, etc. - but these would need
                // more plugins as dependencies just for the tests):
                "        echo \"Set currentBuild.displayName = '${iStrName}'\"\n"
                + "        currentBuild.displayName = iStrName\n"
                +
                // Randomize which job/executor combo waits for which,
                // so we do not have all builds sequentially completing:
                "        sleep (time: 500 + Math.abs(new Random().nextInt(1000)), unit: 'MILLISECONDS')\n"
                + "      dir(\"subdir-${iStr}\".toString()) {\n"
                + "        git url: 'file://' + repoDir\n"
                + "      }\n"
                + "      org.jenkinsci.plugins.githubautostatus.BuildStatusAction buildStatusAction = currentBuild.rawBuild.getAction(org.jenkinsci.plugins.githubautostatus.BuildStatusAction.class)\n"
                + "      if (buildStatusAction != null) {\n"
                + "        buildStatusAction.addBuildStatus(\"stage-${iStr}\".toString())\n"
                + "        buildStatusAction.updateBuildStatusForStage(\"stage-${iStr}\".toString(), org.jenkinsci.plugins.githubautostatus.model.BuildStage.State.CompletedSuccess)\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "parallel parstages\n";

        capturingLogger.addHandler(capturingLogHandler);

        try {
            for (int i = 0; i < maxRuns; i++) {
                WorkflowJob p = r.createProject(WorkflowJob.class);
                p.setDefinition(new CpsFlowDefinition(pipeCode, false));
                wfJobs.add(p);
            }

            LOGGER.info("Execute test workflows");
            for (int i = 0; i < maxRuns; i++) {
                WorkflowRun b = wfJobs.get(i).scheduleBuild2(0).waitForStart();
                // We use a real object here instead of a mock because:
                // 1. We want to test the real synchronization inside BuildStatusAction.
                // 2. XStream serialization (which we're testing for CME) works best with real objects.
                BuildStatusAction buildStatus = BuildStatusAction.newAction(b, r.getURL().toString(), new ArrayList<>());
                b.addOrReplaceAction(buildStatus);
                wfRuns.add(b);
            }

            for (int i = 0; i < maxRuns; i++) {
                r.waitForMessage("[Pipeline] parallel", wfRuns.get(i));
            }

            Jenkins jenkins = Jenkins.get();

            // Trigger Jenkins-wide save activities.
            // Note: job runs also save workflow for good measure
            // Also save state of whole Jenkins config somehow.
            // TOTHINK: Is there more to XStream-able state to save?
            for (int i = 0; i < 10; i++) {
                LOGGER.info("Trigger Jenkins state save (random interval ~3s +- 50ms)");
                jenkins.save();
                // Let the timing be out of sync of ~1s sleeps of the pipelines
                Thread.sleep(2950 + Math.abs(new Random().nextInt(100)));
            }

            for (int i = 0; i < 10; i++) {
                // Let the timing be out of sync of ~1s sleeps of the pipelines
                LOGGER.info("Trigger Jenkins state save (regular interval ~3s later)");
                jenkins.save();
                Thread.sleep(2922);
            }

            LOGGER.info("Wait for builds to complete");
            for (int i = 0; i < maxRuns; i++) {
                r.waitForCompletion(wfRuns.get(i));
            }

            LOGGER.info("All builds completed; will analyze their status and logs below");
        } finally {
            // Complete this bit of ritual even if test run
            // (e.g. build status assertion) throws above
            capturingLogger.removeHandler(capturingLogHandler);
        }

        LOGGER.info("Check statuses of completed builds");
        for (int i = 0; i < maxRuns; i++) {
            r.assertBuildStatusSuccess(wfRuns.get(i));
        }

        LOGGER.info("Check build logs that CME related messages are absent");
        for (int i = 0; i < maxRuns; i++) {
            WorkflowRun b = wfRuns.get(i);
            for (String s : indicatorsCME) {
                r.assertLogNotContains(s, b);
            }
        }

        // Not printed if assertion above fails:
        LOGGER.info("All " + maxRuns + " builds are done successfully and did not report CME");

        if (systemErrTap != null) {
            String stderrLog = null;
            try {
                stderrLog = systemErrTap.getText();
            } catch (NullPointerException npe) {
                // FIXME: seems we can not always collect from systemErrTap...
                //  Maybe due to mixing jupiter and junit4 implementations in
                //  same test class? (consider another variant of system-stubs)
                LOGGER.info("We tapped into JVM stderr but this collected nothing: could not systemErrTap.getText(): " + npe.getMessage());
            }
            if (stderrLog != null && !(stderrLog.trim().isEmpty())) {
                LOGGER.info("Check JVM stderr that CME related messages are absent");
                for (String s : indicatorsCME) {
                    assertFalse(stderrLog.contains(s));
                }
            } else {
                LOGGER.info("We tapped into JVM stderr but this collected nothing: stderrLog is null or blank");
            }
        } else {
            LOGGER.info("We tapped into JVM stderr but this collected nothing: systemErrTap is null");
        }

        LOGGER.info("Check custom Jenkins logger that CME related messages are absent");
        String capturedLog = capturedLogs.toString();
        for (String s : indicatorsCME) {
            assertFalse(capturedLog.contains(s));
        }

        LOGGER.info("SUCCESS: Test completed without catching any indicators of ConcurrentModificationException");
    }
}
