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

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Sends job and stage metrics to a statsd collector server over UDP.
 * 
 * @author Tom Hadlaw (thomas.hadlaw@hootsuite.com)
 */
public class StatsdNotifier implements BuildNotifier {
    private StatsdWrapper client;
    protected StatsdNotifierConfig config;
    private static final Logger LOGGER = Logger.getLogger(StatsdWrapper.class.getName());

    public StatsdNotifier(StatsdWrapper client, StatsdNotifierConfig config) {
        this.client = client;
        this.config = config;
    }

    public StatsdNotifier(StatsdNotifierConfig config) {
        this.config = config;

        client = StatsdClient.getInstance(config.getStatsdBucket(), config.getStatsdHost(), config.getStatsdPort());
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
        String sanitizedExternalizedID = sanitizeAll(config.getExternalizedID());
        return String.format("pipeline.%s", sanitizedExternalizedID);
    }

    /**
     * Sends build status metric to statsd by doing an increment on the buildState
     * categories
     * 
     * @param jobName    name of the job
     * @param nodeName   the stage of the status on which to report on
     * @param buildState the reported state
     */
    public void notifyBuildState(String jobName, String nodeName, BuildState buildState) {
        notifyBuildStageStatus(getBranchPath(), nodeName, buildState, 0);
    }

    /**
     * Sends duration metric to statsd by doing a timer metric
     * 
     * @param jobName  name of the job
     * @param nodeName the stage of the status on which to report on
     * @param buildState the current build stage of the running job
     * @param nodeDuration the duration of the node
     */
    public void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long nodeDuration) {
        if (buildState == BuildState.Pending) {
            return;
        }
        String result = sanitizeAll(buildState.toString());
        int statsDMaxSize = Integer.parseInt(config.getStatsdMaxSize().trim());

        String stageStatus = String.format("%s.stage.%s.status.%s", sanitizeAll(getBranchPath()), sanitizeAll(nodeName), result);
        byte[] stageStatusSize;
        try {
            stageStatusSize = stageStatus.getBytes("UTF-16");
            if (stageStatusSize.length > statsDMaxSize){
                LOGGER.warning("Statsd notify exceeds maxPaketSize for stageStatus");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to find byte size of stageStatus");
            e.printStackTrace();
        }
        client.increment(stageStatus, 1);

        String stageDuration = String.format("%s.stage.%s.duration", sanitizeAll(getBranchPath()), sanitizeAll(nodeName));
        byte[] stageDurationSize;
        try {
            stageDurationSize = stageDuration.getBytes("UTF-16");
            if (stageDurationSize.length > statsDMaxSize){
                LOGGER.warning("Statsd notify exceeds maxPaketSize for stageDuration");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to find byte size of stageDuration");
            e.printStackTrace();
        }
        client.time(stageDuration, nodeDuration);
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
        byte[] fqpSize;
        String result = sanitizeAll(buildState.toString());
        int statsDMaxSize = Integer.parseInt(config.getStatsdMaxSize().trim());

        String fqp = String.format("%s.job.status.%s", sanitizeAll(getBranchPath()), result);
        try {
            fqpSize = fqp.getBytes("UTF-16");
            if (fqpSize.length > statsDMaxSize){
                LOGGER.warning("Statsd notify exceeds maxPaketSize for jobStatus");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to find byte size of jobStatus");
            e.printStackTrace();
        }
        client.increment(fqp, 1);
        

        fqp = String.format("%s.job.duration", sanitizeAll(getBranchPath()));
        try {
            fqpSize = fqp.getBytes("UTF-16");
            if (fqpSize.length > statsDMaxSize){
                LOGGER.warning("Statsd notify exceeds maxPaketSize for duration");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to find byte size of duration");
            e.printStackTrace();
        }
        client.time(fqp, buildDuration);


        fqp = String.format("%s.job.blocked_duration", sanitizeAll(getBranchPath()));
        try {
            fqpSize = fqp.getBytes("UTF-16");
            if (fqpSize.length > statsDMaxSize){
                LOGGER.warning("Statsd notify exceeds maxPaketSize for blockedDuration");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to find byte size of blockedDuration");
            e.printStackTrace();
        }
        client.time(fqp, buildDuration);
    }

    /**
     * Sends build status metric to statsd by doing an increment on the buildState categories
     * 
     * @param jobName name of the job
     * @param nodeName the stage of the status on which to report on
     */
    public void sendNonStageError(String jobName, String nodeName) {
        int statsDMaxSize = Integer.parseInt(config.getStatsdMaxSize().trim());

        String fqp = String.format("%s.stage.%s.non_stage_error", sanitizeAll(getBranchPath()), sanitizeAll(nodeName));
        byte[] fqpSize;
        try {
            fqpSize = fqp.getBytes("UTF-16");
            if (fqpSize.length > statsDMaxSize){
                LOGGER.warning("Statsd notify exceeds maxPaketSize for nonStageError");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to find byte size of nonStageError");
            e.printStackTrace();
        }
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
     * Gets rid of # and trailing characters at the end of the string
     * 
     * @param key key to sanitize
     * @return sanitized key
     */
    private String saninitizeBuildNumber(String key) {
        if (key.indexOf('#') != -1){
            return key.split("#")[0];
        }
        return key;
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
        return collapseEmptyBuckets(statsdSanitizeKey(sanitizeKey(saninitizeBuildNumber(key.toLowerCase()))));
    }
}