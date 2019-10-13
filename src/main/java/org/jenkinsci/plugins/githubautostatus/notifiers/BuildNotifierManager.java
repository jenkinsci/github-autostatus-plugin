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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.githubautostatus.BuildStageModel;
import org.jenkinsci.plugins.githubautostatus.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;

/**
 * Manages send build notifications to one or more notifiers
 *
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
public class BuildNotifierManager {

    final String targetUrl;
    final String jobName;

    List<BuildNotifier> notifiers = new ArrayList<>();

    /**
     * Constructs a BuildNotifierManager
     *
     * @param jobName the job notifications are for
     * @param targetUrl link back to Jenkins
     */
    public BuildNotifierManager(String jobName, String targetUrl) {
        this.jobName = jobName;
        this.targetUrl = targetUrl;
    }

    /**
     * Adds a GitHub repository for notifications
     *
     * @param config GitHub notification configuration
     * @return the notifier which was added
     */
    public BuildNotifier addGithubNotifier(GithubNotificationConfig config) {
        GithubBuildNotifier buildNotifier = new GithubBuildNotifier(config.getRepo(), config.getShaString(), this.targetUrl);
        return addBuildNotifier(buildNotifier);
    }

    /**
     * Adds an InfluxDB notifier
     *
     * @param influxDbNotifierConfig InfluxDB notification configuration
     * @return the notifier which was added
     */
    public BuildNotifier addInfluxDbNotifier(InfluxDbNotifierConfig influxDbNotifierConfig) {
        InfluxDbNotifier buildNotifier = new InfluxDbNotifier(influxDbNotifierConfig);
        return addBuildNotifier(buildNotifier);
    }
    public BuildNotifier addGenericNofifier(BuildNotifier buildNotifier) {
        return addBuildNotifier(buildNotifier);
    }

    /**
     * Adds a StatsD notifier
     *
     * @param statsdNotifierConfig StatsD notification configuration
     * @return the notifier which was added
     */
    public BuildNotifier addStatsdBuildNotifier(StatsdNotifierConfig statsdNotifierConfig) {
        StatsdNotifier buildNotifier = new StatsdNotifier(statsdNotifierConfig);
        return addBuildNotifier(buildNotifier);
    }

    /**
     * Adds a notifier if it's enabled
     *
     * @param notifier notifier to add
     * @return notifier if added; null if not
     */
    BuildNotifier addBuildNotifier(BuildNotifier notifier) {
        if (notifier.isEnabled()) {
            notifiers.add(notifier);
            return notifier;
        }
        return null;
    }

    /**
     * Send stage status notification
     *
     * @param stageItem stage item
     */
    public void notifyBuildStageStatus(BuildStageModel stageItem) {
        notifiers.forEach((notifier) -> {
            notifier.notifyBuildStageStatus(jobName, stageItem);
        });
    }

    /**
     * Send overall build status notification
     *
     * @param buildState the build status
     * @param parameters build parameters
     */
    public void notifyFinalBuildStatus(BuildState buildState, Map<String, Object> parameters) {
        notifiers.forEach((notifier) -> {
            notifier.notifyFinalBuildStatus(buildState, parameters);
        });
    }

    /**
     * Sends a notification for an error regardless of whether initial pending
     * status was sent. Useful for reporting errors for non-declarative
     * pipelines when they happen outside of a stage.
     *
     * @param stageItem stage item
     */
    public void sendNonStageError(BuildStageModel stageItem) {
        notifiers.forEach((notifier) -> {
            notifier.notifyBuildStageStatus(jobName, stageItem);
        });
    }
}
