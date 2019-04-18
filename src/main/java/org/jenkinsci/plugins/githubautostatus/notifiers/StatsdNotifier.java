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

import org.jenkinsci.plugins.githubautostatus.StatsdWrapper;
import org.jenkinsci.plugins.githubautostatus.StatsdClient;
import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;

import java.util.logging.Logger;

/**
 * Sends job and stage metrics to a statsd collector server over UDP.
 * @author Tom Hadlaw (thomas.hadlaw@hootsuite.com)
 */
public class StatsdNotifier implements BuildNotifier {
    private StatsdWrapper client;
    protected StatsdNotifierConfig config;
    private static final Logger LOGGER = Logger.getLogger(StatsdWrapper.class.getName());
    public StatsdNotifier(StatsdWrapper client) {
        this.client = client;
    }

    public StatsdNotifier(StatsdNotifierConfig config) {
        this.config = config;
        int port = 8125;
        if (!config.getStatsdPort().equals("")) {
            try {
                port = Integer.parseInt(config.getStatsdPort());
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not parse port '" + config.getStatsdPort() + "', using 8125 (default)");
            }
        }
        client = new StatsdClient(config.getStatsdBucket(), config.getStatsdHost(), port);
    }

    /**
     * Determine whether notifier is enabled
     *
     * @return true if enabled; false otherwise
     */
    public boolean isEnabled() {
        return this.client != null;
    }

    /**
     * Returns the statsd including the global prefix up to the branch bucket
     * 
     * @return string of path up to branch bucket
     */
    public String getBranchPath() {
        String sanitizedFolderPath = sanitizeAll(config.getRepoOwner());
        String sanitizedJobName = sanitizeAll(config.getRepoName());
        String sanitizedBranchName = sanitizeAll(config.getBranchName());
        return String.format("pipeline.%s.%s.branch.%s", sanitizedFolderPath, sanitizedJobName, sanitizedBranchName);
    }

    /**
     * Sends build status metric to statsd by doing an increment on the buildState categories
     * 
     * @param jobName name of the job
     * @param nodeName the stage of the status on which to report on
     * @param buildState the reported state
     */
    public void notifyBuildState(String jobName, String nodeName, BuildState buildState) {
        String fqp = String.format("%s.stage.%s.status.%s", getBranchPath(), sanitizeAll(nodeName), buildState);  
        client.increment(fqp, 1);
    }

    /**
     * Sends duration metric to statsd by doing a timer metric  
     * 
     * @param jobName name of the job
     * @param nodeName the stage of the status on which to report on
     */
    public void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long nodeDuration) {
        String fqp = String.format("%s.stage.%s.duration", getBranchPath(), sanitizeAll(nodeName));
        client.time(fqp, nodeDuration);
    }

    /**
     * Sends final build status metric by doing a timer metric for blocked and unblocked job time 
     * 
     * @param jobName name of the job
     * @param buildState the reported state
     * @param buildDuration the duration of the build
     * @param blockedDuration the blocked duration of the build
     */
    public void notifyFinalBuildStatus(String jobName, BuildState buildState, long buildDuration, long blockedDuration) {
        String fqp = String.format("%s.job.status.%s", getBranchPath(), buildState);
        client.increment(fqp, 1);
        fqp = String.format("%s.job.duration", getBranchPath());
        client.time(fqp, buildDuration);
        fqp = String.format("%s.job.blocked_duration", getBranchPath());
        client.time(fqp, buildDuration);
    }

    /**
     * Sends build status metric to statsd by doing an increment on the buildState categories
     * 
     * @param jobName name of the job
     * @param nodeName the stage of the status on which to report on
     */
    public void sendNonStageError(String jobName, String nodeName) {
        String fqp = String.format("%s.stage.%s.none_stage_error", getBranchPath(), sanitizeAll(nodeName));  
        client.increment(fqp, 1);
    }

    /**
     * Does the same sanitization as Statsd would do if sanitization is on.
     * See: https://github.com/statsd/statsd/blob/master/stats.js#L168
     * 
     * @param key key to sanitize
     * @return santized key
     */
    private String statsdSanitizeKey(String key) {
        return key.replaceAll("\\s+", "_").replaceAll("/", ".").replaceAll("[^a-zA-Z_\\-0-9\\.]", "");
    }

    /**
     * Does Jenkins specific key sanitization.
     * 
     * @param key key to sanitize
     * @return sanitized key
     */
    private String sanitizeKey(String key) {
        return key.replaceAll("\\.", "");
    }

    /**
     * Applies all sanitizations to a key, folders are expanded into seperate statsd buckets.
     * It firest applies bucket sanitization (removing periods to prevent them being interprested as 
     * seperate buckets). It the applies the statsd bucket key sanitization.
     * 
     * @param key key to sanitize
     * @return sanitized key
     */
    public String sanitizeAll(String key) {
        return statsdSanitizeKey(sanitizeKey(key));
    }
}