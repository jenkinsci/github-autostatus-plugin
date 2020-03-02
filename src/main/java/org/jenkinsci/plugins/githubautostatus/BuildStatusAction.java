/*
 * The MIT License
 *
 * Copyright 2017 Jeff Pearce (jeffpea@gmail.com).
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

import hudson.ExtensionList;
import hudson.model.InvisibleAction;
import hudson.model.JobProperty;
import hudson.model.Run;
import org.jenkinsci.plugins.githubautostatus.config.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.config.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.config.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifier;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierConstants;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierManager;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps track of build status for each stage in a build, and provides
 * mechanisms for notifying various subscribers as stages and jobs are
 * completed.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildStatusAction extends InvisibleAction {

    private final String jobName;
    private boolean isDeclarativePipeline;
    private String repoOwner;
    private String repoName;
    private String branchName;
    private Run<?, ?> run;
    private HashMap<String, Object> jobParameters;

    private final HashMap<String, BuildStage> buildStatuses;

    protected transient BuildNotifierManager buildNotifierManager;

    public String getJobName() {
        return jobName;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    /**
     * Construct a BuildStatusAction
     *
     * @param run       the build
     * @param targetUrl link back to Jenkins
     * @param stageList list of stages if known
     */
    public static BuildStatusAction newAction (Run<?, ?> run, String targetUrl, List<BuildStage> stageList) {
        return new BuildStatusAction(run, targetUrl, stageList);
    }

    protected BuildStatusAction(Run<?, ?> run, String targetUrl, List<BuildStage> stageList) {
        this.run = run;
        this.jobName = run.getExternalizableId();
        this.buildStatuses = new HashMap<>();
        this.jobParameters = new HashMap<>();
        addGlobalProperties();
        stageList.forEach((stageItem) -> {
            stageItem.setRun(run);
            stageItem.addAllToEnvironment(jobParameters);
            buildStatuses.put(stageItem.getStageName(), stageItem);
        });
        connectNotifiers(run, targetUrl);
    }

    /**
     * Determines whether the notifiers need to be reconnected. This is necessary because the GitHub notifier
     * can't be serialized because of the JEP-200 security improvements. In the event the build is interrupted and
     * the buildAction is loaded from disk, the notifiers need to be added again.
     *
     * @param run       the current build
     * @param targetUrl link back to Jenkins
     */
    public void connectNotifiers(Run<?, ?> run, String targetUrl) {
        if (buildNotifierManager != null) {
            return;
        }
        buildNotifierManager = BuildNotifierManager.newInstance(jobName, targetUrl);

        GithubNotificationConfig githubConfig = GithubNotificationConfig.fromRun(run);
        if (githubConfig != null) {
            addGithubNotifier(githubConfig);
            repoOwner = githubConfig.getRepoOwner();
            repoName = githubConfig.getRepoName();
            branchName = githubConfig.getBranchName();
        } else {
            if (run instanceof WorkflowRun) {
                repoName = run.getParent().getDisplayName();
                repoOwner = run.getParent().getParent().getFullName();
            }
        }

        addInfluxDbNotifier(InfluxDbNotifierConfig.fromGlobalConfig(repoOwner, repoName, branchName));
        StatsdNotifierConfig statsd = StatsdNotifierConfig.fromGlobalConfig(run.getExternalizableId());
        if (statsd != null) {
            addStatsdNotifier(statsd);
        }
        addHttpNotifier(HttpNotifierConfig.fromGlobalConfig(repoOwner, repoName, branchName));

        ExtensionList<BuildNotifier> list = BuildNotifier.all();
        for (BuildNotifier notifier : list) {
            addGenericNotifier(notifier);
        }
    }

    private void addGlobalProperties() {
        if (run instanceof WorkflowRun) {
            WorkflowRun workflowRun = (WorkflowRun) run;
            List<JobProperty<? super WorkflowJob>> properties = workflowRun.getParent().getAllProperties();
            for (JobProperty property : properties) {
                jobParameters.put(property.getClass().getSimpleName(), property);
            }
        }
    }

    /**
     * Cleans up by sending "complete" status to any steps that are currently
     * pending. Needed because some complex jobs, particularly using down
     */
    public void close() {
        this.buildStatuses.forEach((nodeName, stageItem) -> {
            if (stageItem.getBuildState() == BuildStage.State.Pending) {
                this.updateBuildStatusForStage(nodeName, BuildStage.State.CompletedSuccess);
            }
        });
    }

    /**
     * Sets flag indicating whether notifications are for a declarative pipeline
     *
     * @return if pipeline is declarative or not
     */
    public boolean isIsDeclarativePipeline() {
        return isDeclarativePipeline;
    }

    public void setIsDeclarativePipeline(boolean isDeclarativePipeline) {
        this.isDeclarativePipeline = isDeclarativePipeline;
    }

    /**
     * Attempts to add a GitHub notifier.
     *
     * @param config GitHub notifier config
     */
    public void addGithubNotifier(GithubNotificationConfig config) {
        if (config != null) {
            sendNotifications(buildNotifierManager.addGithubNotifier(config));
        }
    }

    /**
     * Attempts to add an InfluxDB notifier.
     *
     * @param influxDbNotifierConfig InfluxDB notifier config
     */
    public void addInfluxDbNotifier(InfluxDbNotifierConfig influxDbNotifierConfig) {
        sendNotifications(buildNotifierManager.addInfluxDbNotifier(influxDbNotifierConfig));
    }

    /**
     * Attempts to add a StatsD notifier.
     *
     * @param statsdNotifierConfig StatsD notifier config
     */
    public void addStatsdNotifier(StatsdNotifierConfig statsdNotifierConfig) {
        BuildNotifier build = buildNotifierManager.addStatsdBuildNotifier(statsdNotifierConfig);
        sendNotifications(build);
    }

    /**
     * Attempts to add an HTTP notifier.
     *
     * @param httpNotifierConfig HTTP notifier config
     */
    public void addHttpNotifier(HttpNotifierConfig httpNotifierConfig) {
        sendNotifications(buildNotifierManager.addHttpNotifier(httpNotifierConfig));
    }

    public void addGenericNotifier(BuildNotifier notifier) {
        sendNotifications(buildNotifierManager.addGenericNotifier(notifier));
    }

    /**
     * Sends all saved notifications to a notifier.
     *
     * @param notifier notifier to send to
     */
    public void sendNotifications(BuildNotifier notifier) {
        if (notifier != null && notifier.isEnabled()) {
            this.buildStatuses.forEach((nodeName, stageItem) -> {
                stageItem.setRun(run);
                notifier.notifyBuildStageStatus(jobName, stageItem);
            });
        }
    }

    /**
     * Sends pending notifications for the start of a stage.
     *
     * @param stageName stage name
     */
    public void addBuildStatus(String stageName) {
        BuildStage stageItem = new BuildStage(stageName);
        stageItem.setRun(run);
        buildStatuses.put(stageName, stageItem);
        buildNotifierManager.notifyBuildStageStatus(stageItem);
    }

    /**
     * Sends notifications for a completed stage.
     *
     * @param nodeName   node name
     * @param buildState build state
     * @param time       stage time
     */
    public void updateBuildStatusForStage(String nodeName, BuildStage.State buildState, long time) {
        BuildStage stageItem = buildStatuses.get(nodeName);
        if (stageItem != null) {
            stageItem.addToEnvironment(BuildNotifierConstants.STAGE_DURATION, time);
            BuildStage.State currentStatus = stageItem.getBuildState();
            if (currentStatus == BuildStage.State.Pending) {
                stageItem.setBuildState(buildState);
                buildNotifierManager.notifyBuildStageStatus(stageItem);
            }
        }
    }

    /**
     * Sends notifications for a completed stage.
     *
     * @param nodeName   node name
     * @param buildState build state
     */
    public void updateBuildStatusForStage(String nodeName, BuildStage.State buildState) {
        updateBuildStatusForStage(nodeName, buildState, 0);
    }

    /**
     * Sends notifications for final build status.
     *
     * @param buildState final build state
     * @param parameters build parameters
     */
    public void updateBuildStatusForJob(BuildStage.State buildState, Map<String, Object> parameters) {
        close();
        buildNotifierManager.notifyFinalBuildStatus(buildState, parameters);
    }

    /**
     * Sends notifications for an error that happens outside of a stage.
     *
     * @param nodeName name of node that failed
     */
    public void sendNonStageError(String nodeName) {
        if (buildStatuses.get(nodeName) != null) {
            // We already reported this error
            return;
        }
        BuildStage stageItem = new BuildStage(nodeName, new HashMap<>(), BuildStage.State.CompletedError);
        stageItem.setRun(run);
        stageItem.addAllToEnvironment(jobParameters);
        stageItem.setIsStage(false);
        buildStatuses.put(nodeName, stageItem);
        buildNotifierManager.sendNonStageError(stageItem);
    }
}
