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
import org.jenkinsci.plugins.githubautostatus.BuildStageModel;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

/**
 * Sets the GitHub commit status for stages based on build notifications.
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
public class GithubBuildNotifier extends BuildNotifier {

    private final GHRepository repository;
    private final String shaString;
    private final String targetUrl;

    static final ImmutableMap<BuildState, GHCommitState> STATE_MAP = new ImmutableMap.Builder()
            .put(BuildState.Pending, GHCommitState.PENDING)
            .put(BuildState.CompletedError, GHCommitState.ERROR)
            .put(BuildState.CompletedSuccess, GHCommitState.SUCCESS)
            .put(BuildState.SkippedFailure, GHCommitState.SUCCESS)
            .put(BuildState.SkippedUnstable, GHCommitState.SUCCESS)
            .put(BuildState.SkippedConditional, GHCommitState.SUCCESS)
            .build();

    static final ImmutableMap<BuildState, String> DESCRIPTION_MAP = new ImmutableMap.Builder()
            .put(BuildState.Pending, "Building stage")
            .put(BuildState.CompletedError, "Failed to build stage")
            .put(BuildState.CompletedSuccess, "Stage built successfully")
            .put(BuildState.SkippedFailure, "Stage did not run due to earlier failure(s)")
            .put(BuildState.SkippedUnstable, "Stage did not run due to earlier stage(s) marking the build as unstable")
            .put(BuildState.SkippedConditional, "Stage did not run due to when conditional")
            .build();

    /**
     * Constructor
     *
     * @param repository the GitHub repository
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
     * @param jobName the job  name
     * @param stageItem stage item describing the new state
     */
    @Override
    public void notifyBuildStageStatus(String jobName, BuildStageModel stageItem) {
        try {
            BuildState buildState = stageItem.getBuildState();
            repository.createCommitStatus(shaString, STATE_MAP.get(buildState), targetUrl, DESCRIPTION_MAP.get(buildState), stageItem.getStageName());
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
     * Send a notification when the job is complete
     *
     * @param buildState state indicating success or failure
     * @param parameters build parameters
     */
    @Override
    public void notifyFinalBuildStatus(BuildState buildState, Map<String, Object> parameters) {
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
