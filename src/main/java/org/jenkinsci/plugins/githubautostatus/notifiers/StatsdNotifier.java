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

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import org.jenkinsci.plugins.githubautostatus.StatsdClient;
import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.StatsdWrapper;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;

/**
 * Sends job and stage metrics to a StatsD collector server over UDP.
 *
 * @author Tom Hadlaw (thomas.hadlaw@hootsuite.com)
 */
public class StatsdNotifier extends BuildNotifier {

    private StatsdWrapper client;
    protected StatsdNotifierConfig config;

    // Used only for testing
    public StatsdNotifier(StatsdWrapper client, StatsdNotifierConfig config) {
        this.client = client;
        this.config = config;
    }

    public StatsdNotifier(StatsdNotifierConfig config) {
        this.config = config;

        client = StatsdClient.getInstance(config.getStatsdBucket(), config.getStatsdHost(), config.getStatsdPort());
    }

    /**
     * Determines whether this notifier is enabled.
     *
     * @return true if enabled; false otherwise
     */
    public boolean isEnabled() {
        return this.client != null;
    }

    /**
     * Returns the StatsD including the global prefix up to the branch bucket.
     *
     * @return string of path up to branch bucket
     */
    public String getBranchPath() {
        String sanitizedExternalizedID = sanitizeAll(config.getExternalizedID());
        return String.format("pipeline.%s", sanitizedExternalizedID);
    }

    /**
     * Sends duration metric to StatsD by doing a timer metric.
     *
     * @param jobName   the name of the job
     * @param stageItem stage item describing the new state
     */
    public void notifyBuildStageStatus(String jobName, BuildStage stageItem) {
        BuildStage.State buildState = stageItem.getBuildState();

        if (buildState == BuildStage.State.Pending) {
            return;
        }

        Object timingInfo = stageItem.getDuration();
        String nodeName = stageItem.getStageName();
        long nodeDuration;
        try {
            nodeDuration = (long) timingInfo;
        } catch (NullPointerException e) {
            nodeDuration = 0;
        }
        // public void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long nodeDuration) {

        String result = buildState.toString();
        int statsDMaxSize = Integer.parseInt(config.getStatsdMaxSize().trim());

        String stageStatus = String.format("%s.stage.%s.status.%s", getBranchPath(), sanitizeAll(nodeName), sanitizeAll(result));
        byte[] stageStatusSize;
        stageStatusSize = stageStatus.getBytes(StandardCharsets.UTF_16);
        if (stageStatusSize.length > statsDMaxSize) {
            log(Level.INFO, "StatsD notify exceeds max. packet size for stageStatus");
        }
        client.increment(stageStatus, 1);

        String stageDuration = String.format("%s.stage.%s.duration", getBranchPath(), sanitizeAll(nodeName));
        byte[] stageDurationSize;
        stageDurationSize = stageDuration.getBytes(StandardCharsets.UTF_16);
        if (stageDurationSize.length > statsDMaxSize) {
            log(Level.WARNING, "StatsD notify exceeds max. packet size for stageDuration");
        }
        client.time(stageDuration, nodeDuration);
    }

    /**
     * Sends final build status metric by doing a timer metric for blocked and unblocked job time.
     *
     * @param buildState the reported state
     * @param parameters build parameters
     */
    public void notifyFinalBuildStatus(BuildStage.State buildState, Map<String, Object> parameters) {
        long blockedDuration = getLong(parameters, BuildNotifierConstants.BLOCKED_DURATION);
        long buildDuration = getLong(parameters, BuildNotifierConstants.JOB_DURATION) - blockedDuration;
        byte[] fqpSize;
        String result = sanitizeAll(buildState.toString());
        int statsDMaxSize = Integer.parseInt(config.getStatsdMaxSize().trim());

        String fqp = String.format("%s.job.status.%s", getBranchPath(), result);
        fqpSize = fqp.getBytes(StandardCharsets.UTF_16);
        if (fqpSize.length > statsDMaxSize) {
            log(Level.WARNING, "StatsD notify exceeds max. packet size for jobStatus");
        }
        client.increment(fqp, 1);


        fqp = String.format("%s.job.duration", getBranchPath());
        fqpSize = fqp.getBytes(StandardCharsets.UTF_16);
        if (fqpSize.length > statsDMaxSize) {
            log(Level.WARNING, "StatsD notify exceeds max. packet size for duration");
        }
        client.time(fqp, buildDuration);


        fqp = String.format("%s.job.blocked_duration", getBranchPath());
        fqpSize = fqp.getBytes(StandardCharsets.UTF_16);
        if (fqpSize.length > statsDMaxSize) {
            log(Level.WARNING, "StatsD notify exceeds max. packet size for blockedDuration");
        }
        client.time(fqp, blockedDuration);
    }

    /**
     * Sends build status metric to StatsD by doing an increment on the buildState categories.
     *
     * @param jobName name of the job
     * @param nodeName the stage of the status on which to report on
     */
    public void sendNonStageError(String jobName, String nodeName) {
        int statsDMaxSize = Integer.parseInt(config.getStatsdMaxSize().trim());

        String fqp = String.format("%s.stage.%s.non_stage_error", getBranchPath(), sanitizeAll(nodeName));
        byte[] fqpSize;
        fqpSize = fqp.getBytes(StandardCharsets.UTF_16);
        if (fqpSize.length > statsDMaxSize) {
            log(Level.WARNING, "StatsD notify exceeds max. packet size for nonStageError");
        }
        client.increment(fqp, 1);
    }

    /**
     * Does the same sanitization as StatsD would do if sanitization is on.
     * See: https://github.com/statsd/statsd/blob/master/stats.js#L168
     *
     * @param key key to sanitize
     * @return sanitized key
     */
    private String statsdSanitizeKey(String key) {
        return key.replaceAll("\\s+", "_").replaceAll("/", ".").replaceAll("[^a-z_\\-0-9\\.]", "");
    }

    /**
     * Collapses empty buckets into dot.
     *
     * @param key key to sanitize
     * @return sanitized key
     */
    private String collapseEmptyBuckets(String key) {
        return key.replaceAll("\\.{2,}", ".");
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
     * Gets rid of # and trailing characters at the end of the string.
     *
     * @param key key to sanitize
     * @return sanitized key
     */
    private String sanitizeBuildNumber(String key) {
        if (key.indexOf('#') != -1) {
            return key.split("#")[0];
        }
        return key;
    }

    /**
     * Applies all sanitizations to a key, folders are expanded into separate StatsD buckets.
     * It first applies bucket sanitization (removing periods to prevent them being interpreted as
     * separate buckets). It then applies the StatsD bucket key sanitization.
     *
     * @param key key to sanitize
     * @return sanitized key
     */
    public String sanitizeAll(String key) {
        return collapseEmptyBuckets(statsdSanitizeKey(sanitizeKey(sanitizeBuildNumber(key.toLowerCase()))));
    }

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(StatsdClient.class.getName());
    }
}
