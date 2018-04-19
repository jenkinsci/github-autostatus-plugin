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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

/**
 *
 * @author jxpearce
 */
public class GithubBuildNotifier implements BuildNotifier {

    private final GHRepository repository;
    private final String shaString;
    private final String targetUrl;

    private final Map<BuildState, GHCommitState> STATE_MAP = ImmutableMap.of(
            BuildState.Pending, GHCommitState.PENDING,
            BuildState.CompletedError, GHCommitState.ERROR,
            BuildState.CompletedSuccess, GHCommitState.SUCCESS);

    static final Map<BuildState, String> DESCRIPTION_MAP = ImmutableMap.of(
            BuildState.Pending, "Building stage",
            BuildState.CompletedError, "Failed to build stage",
            BuildState.CompletedSuccess, "Stage built successfully");

    /**
     * Constructor
     *
     * @param repository the github repository
     * @param shaString the commit notifications are being provided for
     * @param targetUrl target Url (link back to Jenkins)
     */
    public GithubBuildNotifier(GHRepository repository, String shaString, String targetUrl) {
        this.repository = repository;
        this.shaString = shaString;
        this.targetUrl = targetUrl;
    }

    /**
     * Determine whether notifier is enabled
     *
     * @return true if enabled; false otherwise
     */
    @Override
    public boolean isEnabled() {
        return repository != null;
    }

    /**
     * Send stage status notification to github
     *
     * @param nodeName the node that has changed
     * @param buildState the new state
     */
    @Override
    public void notifyBuildState(String jobName, String nodeName, BuildState buildState) {
        try {
            repository.createCommitStatus(shaString, STATE_MAP.get(buildState), targetUrl, DESCRIPTION_MAP.get(buildState), nodeName);
        } catch (org.kohsuke.github.HttpException ex) {
            if (ex.getResponseCode() < 200 || ex.getResponseCode() > 299) {
                log(Level.SEVERE, "Exception while creating status for job %s", jobName);
                log(Level.SEVERE, ex);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "Exception while creating status for job %s", jobName);
            log(Level.SEVERE, ex);
        }
    }

    /**
     * Send stage status notification to github
     *
     * @param nodeName the stage name
     * @param buildState the build status
     * @param nodeDuration elapsed time for this node
     */
    @Override
    public void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long nodeDuration) {
        notifyBuildState(jobName, nodeName, buildState);
    }

    /**
     * Send a notification when the job is complete
     *
     * @param jobName the name of the job
     * @param buildState state indicating success or failure
     * @param buildDuration the build duration
     */
    @Override
    public void notifyFinalBuildStatus(String jobName, BuildState buildState, long buildDuration) {

    }

    /**
     * Sends a notification for an error regardless of whether initial pending
     * status was sent. Useful for reporting errors for non-declarative
     * pipelines since they can happen outside of a stage.
     *
     * @param jobName the name of the job
     * @param nodeName the name of the node that failed
     */
    @Override
    public void sendOutOfBandError(String jobName, String nodeName) {

    }

    private static void log(Level level, Throwable exception) {
        getLogger().log(level, null, exception);
    }

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(InfluxDbNotifier.class.getName());
    }
}
