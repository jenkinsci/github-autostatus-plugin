/*
 * The MIT License
 *
 * Copyright 2017 Jeff Pearce (jxpearce@godaddy.com).
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

import hudson.model.InvisibleAction;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifier;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierManager;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildState;

/**
 * Provides stage and job status updates for a build
 *
 * @author jxpearce
 */
public class BuildStatusAction extends InvisibleAction {

    private final String jobName;
    private boolean isDeclarativePipeline;

    private final HashMap<String, BuildState> buildStatuses;

    private final transient BuildNotifierManager buildNotifierManager;

    /**
     * Construct a BuildStatusAction
     *
     * @param jobName the name of the job status is for
     * @param targetUrl link back to Jenkins
     * @param stageList list of stages if known
     * @throws IOException
     */
    public BuildStatusAction(String jobName, String targetUrl, List<String> stageList) throws IOException {
        this.jobName = jobName;
        this.buildStatuses = new HashMap<>();
        buildNotifierManager = new BuildNotifierManager(jobName, targetUrl);
        stageList.forEach((stageName) -> {
            buildStatuses.put(stageName, BuildState.Pending);
        });
    }

    /**
     * Cleans up by sending "complete" status to any steps that are currently
     * pending. Needed because some complex jobs, particularly using down
     */
    public void close() {
        this.buildStatuses.forEach((nodeName, buildState) -> {
            if (buildState == BuildState.Pending) {
                this.updateBuildStatusForStage(nodeName, BuildState.CompletedSuccess);
            }
        });
    }

    /**
     * Sets flag indicating whether notifications are for a declarative pipeline
     *
     * @return
     */
    public boolean isIsDeclarativePipeline() {
        return isDeclarativePipeline;
    }

    public void setIsDeclarativePipeline(boolean isDeclarativePipeline) {
        this.isDeclarativePipeline = isDeclarativePipeline;
    }

    /**
     * Attempts to add a github notifier
     *
     * @param config github notifier config
     */
    public void addGithubNofifier(GithubNotificationConfig config) {
        if (config != null) {
            sendNotications(buildNotifierManager.addGithubNotifier(config));
        }
    }

    /**
     * Attempts to add an influx db notifier
     *
     * @param influxDbNotifierConfig influx db notifier config
     */
    public void addInfluxDbNotifier(InfluxDbNotifierConfig influxDbNotifierConfig) {
        sendNotications(buildNotifierManager.addInfluxDbNotifier(influxDbNotifierConfig));
    }

    /**
     * Sends all saved notifications to a notifier
     *
     * @param notifier notifier to send to
     */
    public void sendNotications(BuildNotifier notifier) {
        if (notifier != null && notifier.isEnabled()) {
            this.buildStatuses.forEach((nodeName, buildState)
                    -> notifier.notifyBuildState(jobName, nodeName, buildState));
        }
    }

    /**
     * Sends pending notifications for the start of a stage
     *
     * @param stageName stage name
     */
    public void addBuildStatus(String stageName) {
        buildStatuses.put(stageName, BuildState.Pending);
        buildNotifierManager.notifyBuildStageStatus(stageName, BuildState.Pending, 0);
    }

    /**
     * Sends notifications for a completed stage
     *
     * @param nodeName node name
     * @param buildState build state
     * @param time stage time
     */
    public void updateBuildStatusForStage(String nodeName, BuildState buildState, long time) {
        BuildState currentStatus = buildStatuses.get(nodeName);
        if (currentStatus != null && currentStatus == BuildState.Pending) {
            buildNotifierManager.notifyBuildStageStatus(nodeName, buildState, time);
            buildStatuses.put(nodeName, buildState);
        }
    }

    /**
     * Sends notifications for a completed stage
     *
     * @param nodeName node name
     * @param buildState build state
     */
    public void updateBuildStatusForStage(String nodeName, BuildState buildState) {
        updateBuildStatusForStage(nodeName, buildState, 0);
    }

    /**
     * Sends notifications for final build status
     *
     * @param buildState final build state
     * @param buildDuration build duration
     */
    public void updateBuildStatusForJob(BuildState buildState, long buildDuration) {
        close();
        buildNotifierManager.notifyFinalBuildStatus(buildState, buildDuration);
    }

    /**
     * Sends notifications for an error that happens outside of a stage
     *
     * @param nodeName name of node
     */
    public void sendOutOfBandError(String nodeName) {
        buildStatuses.put(nodeName, BuildState.CompletedError);
        buildNotifierManager.sendOutOfBandError(nodeName);
    }
}
