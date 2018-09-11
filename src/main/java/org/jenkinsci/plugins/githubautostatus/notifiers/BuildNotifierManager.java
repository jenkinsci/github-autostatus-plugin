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
import org.jenkinsci.plugins.githubautostatus.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.InfluxDbNotifierConfig;

/**
 * Manages send build notifications to one or more notifiers
 *
 * @author jxpearce
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
     * Adds a Github repository for notifications
     *
     * @param config Github notification configuration
     *
     * @return The notifier object
     */
    public BuildNotifier addGithubNotifier(GithubNotificationConfig config) {
        GithubBuildNotifier buildNotifier = new GithubBuildNotifier(config.getRepo(), config.getShaString(), this.targetUrl);
        return addBuildNotifier(buildNotifier);
    }

    /**
     * Adds an influx DB notifier
     *
     * @param influxDbNotifierConfig influx db notification configuration
     * @return
     */
    public BuildNotifier addInfluxDbNotifier(InfluxDbNotifierConfig influxDbNotifierConfig) {
        InfluxDbNotifier buildNotifier = new InfluxDbNotifier(influxDbNotifierConfig);
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
     * @param stageName the stage name
     * @param buildState the build status
     * @param nodeDuration elapsed time for this node
     */
    public void notifyBuildStageStatus(String stageName, BuildState buildState, long nodeDuration) {
        notifiers.forEach((notifier) -> {
            notifier.notifyBuildStageStatus(jobName, stageName, buildState, nodeDuration);
        });
    }

    /**
     * Send overall build status notification
     *
     * @param buildState the build status
     * @param blockedDuration time build was blocked before running
     */
    public void notifyFinalBuildStatus(BuildState buildState, long buildDuration, long blockedDuration) {
        notifiers.forEach((notifier) -> {
            notifier.notifyFinalBuildStatus(jobName, buildState, buildDuration, blockedDuration);
        });
    }

    /**
     * Sends a notification for an error regardless of whether initial pending
     * status was sent. Useful for reporting errors for non-declarative
     * pipelines when they happen outside of a stage.
     *
     * @param nodeName the name of the node that failed
     */
    public void sendNonStageError(String nodeName) {
        notifiers.forEach((notifier) -> {
            notifier.sendNonStageError(jobName, nodeName);
        });
    }
}
