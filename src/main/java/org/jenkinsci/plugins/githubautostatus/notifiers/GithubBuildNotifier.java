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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.githubautostatus.config.GithubNotificationConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

/**
 * Sets the GitHub commit status for stages based on build notifications.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class GithubBuildNotifier extends BuildNotifier {

    private volatile GHRepository repository;
    private final String shaString;
    private final String targetUrl;
    private final GithubNotificationConfig config;
    private volatile long repositoryCreatedAt;

    /**
     * GitHub App tokens expire after 60 minutes. Re-resolve credentials
     * if the cached repository is older than this threshold.
     */
    static final long CREDENTIAL_REFRESH_MILLIS = 50 * 60 * 1000L; // 50 minutes

    /** Maximum commit status context length accepted by GitHub (we keep it in worst-case JSON bytes to be safe). */
    static final int MAX_CONTEXT_LENGTH = 255;

    static final ImmutableMap<BuildStage.State, GHCommitState> STATE_MAP = new ImmutableMap.Builder<
                    BuildStage.State, GHCommitState>()
            .put(BuildStage.State.Pending, GHCommitState.PENDING)
            .put(BuildStage.State.CompletedError, GHCommitState.ERROR)
            .put(BuildStage.State.CompletedSuccess, GHCommitState.SUCCESS)
            .put(BuildStage.State.SkippedFailure, GHCommitState.SUCCESS)
            .put(BuildStage.State.SkippedUnstable, GHCommitState.SUCCESS)
            .put(BuildStage.State.SkippedConditional, GHCommitState.SUCCESS)
            .build();

    static final ImmutableMap<BuildStage.State, String> DESCRIPTION_MAP = new ImmutableMap.Builder<
                    BuildStage.State, String>()
            .put(BuildStage.State.Pending, "Building stage")
            .put(BuildStage.State.CompletedError, "Failed to build stage")
            .put(BuildStage.State.CompletedSuccess, "Stage built successfully")
            .put(BuildStage.State.SkippedFailure, "Stage did not run due to earlier failure(s)")
            .put(
                    BuildStage.State.SkippedUnstable,
                    "Stage did not run due to earlier stage(s) marking the build as unstable")
            .put(BuildStage.State.SkippedConditional, "Stage did not run due to when conditional")
            .build();

    /**
     * Constructor
     *
     * @param repository the GitHub repository
     * @param shaString the commit notifications are being provided for
     * @param targetUrl target Url (link back to Jenkins)
     */
    public GithubBuildNotifier(GHRepository repository, String shaString, String targetUrl) {
        this(repository, shaString, targetUrl, null);
    }

    /**
     * Constructor with config for credential refresh support.
     *
     * @param repository the GitHub repository
     * @param shaString the commit notifications are being provided for
     * @param targetUrl target Url (link back to Jenkins)
     * @param config config for re-resolving credentials when tokens expire
     */
    public GithubBuildNotifier(
            GHRepository repository, String shaString, String targetUrl, GithubNotificationConfig config) {
        this.repository = repository;
        this.shaString = shaString;
        this.targetUrl = targetUrl;
        this.config = config;
        this.repositoryCreatedAt = System.currentTimeMillis();
    }

    /**
     * Determines whether this notifier is enabled.
     *
     * @return true if enabled; false otherwise
     */
    @Override
    public boolean isEnabled() {
        return repository != null;
    }

    /**
     * Sends stage status notification to GitHub.
     *
     * @param jobName   the job name (used in failure logging,
     *                  not in the {@code createCommitStatus} call)
     * @param stageItem stage item describing the new state;
     *                  its {@link BuildStage#getBuildState() name}
     *                  is used as the optional {@link GHCommitState state},
     *                  and its {@link BuildStage#getStageName() name}
     *                  is used as the optional {@code context} for
     *                  {@link GHRepository#createCommitStatus(String, GHCommitState, String, String, String)}
     *                  ultimate call.
     */
    @Override
    public void notifyBuildStageStatus(String jobName, BuildStage stageItem) {
        if (!stageItem.isStage()) {
            return;
        }
        try {
            createGithubStatus(getRepository(), stageItem);
        } catch (org.kohsuke.github.HttpException ex) {
            if (ex.getResponseCode() == 401 && config != null) {
                // Token likely expired (GitHub App tokens have a 60-minute TTL).
                // Force credential re-resolution and retry once.
                log(Level.INFO, "Received HTTP 401 for job %s, refreshing GitHub credentials and retrying", jobName);
                try {
                    refreshRepository();
                    createGithubStatus(repository, stageItem);
                } catch (Exception retryEx) {
                    log(Level.SEVERE, "Retry after credential refresh failed for job %s", jobName);
                    log(Level.SEVERE, retryEx);
                }
            } else if (ex.getResponseCode() < 200 || ex.getResponseCode() > 299) {
                log(Level.SEVERE, "Exception while creating status for job %s", jobName);
                log(Level.SEVERE, ex);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "Exception while creating status for job %s", jobName);
            log(Level.SEVERE, ex);
        }
    }

    /**
     * Creates the commit status using the stage name as the context "as is".
     * If GitHub rejects it as too long, retries once with a shortened context.
     */
    private void createGithubStatus(GHRepository repo, BuildStage stageItem) throws IOException {
        BuildStage.State buildState = stageItem.getBuildState();
        String context = stageItem.getStageName();
        try {
            repo.createCommitStatus(
                    shaString, STATE_MAP.get(buildState), targetUrl, DESCRIPTION_MAP.get(buildState), context);
        } catch (org.kohsuke.github.HttpException ex) {
            if (ex.getResponseCode() != 422
                    || context == null
                    || ex.getMessage() == null
                    || !ex.getMessage().contains("Context is too long")) {
                throw ex;
            }
            String shortContext = shortenContext(context);
            if (shortContext.equals(context)) {
                // Nothing to shorten, so a retry would fail the same way
                throw ex;
            }
            log(
                    Level.INFO,
                    "GitHub rejected status context of %d characters, retrying with: %s",
                    context.length(),
                    shortContext);
            repo.createCommitStatus(
                    shaString, STATE_MAP.get(buildState), targetUrl, DESCRIPTION_MAP.get(buildState), shortContext);
        }
    }

    /**
     * Worst-case size of one code point once it is a part of a JSON string
     * in the request body, in bytes: {@code "} and {@code \} are escaped
     * with a backslash, control characters may become {@code \}{@code u00XX},
     * and non-ASCII characters are either raw UTF-8 or {@code \}{@code uXXXX}
     * escapes (one per UTF-16 unit) depending on the serializer, so we assume
     * the larger of these.
     */
    static int jsonCost(int codePoint) {
        if (codePoint == '"' || codePoint == '\\') {
            return 2;
        }
        if (codePoint < 0x20) {
            return 6;
        }
        if (codePoint < 0x80) {
            return 1;
        }
        int utf8 = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
        return Math.max(utf8, 6 * Character.charCount(codePoint));
    }

    /** Worst-case size of the string as a JSON value, see {@link #jsonCost(int)}. */
    static int jsonCost(String s) {
        return s.codePoints().map(GithubBuildNotifier::jsonCost).sum();
    }

    /**
     * Shortens a context so that even in the worst case of its JSON
     * representation (see {@link #jsonCost(int)}) it takes at most
     * {@link #MAX_CONTEXT_LENGTH} bytes, to be safe if GitHub counts bytes
     * rather than characters, or the markup/escapes in the request body.
     * Whole code points are kept or dropped, so a multi-byte character
     * is never cut in the middle. A shortened value ends with a short hash
     * of the full value, so that distinct long stage names (which often
     * differ only in their tail) stay distinct.
     */
    static String shortenContext(String context) {
        if (context == null || jsonCost(context) <= MAX_CONTEXT_LENGTH) {
            return context;
        }
        String hash = UUID.nameUUIDFromBytes(context.getBytes(StandardCharsets.UTF_8))
                .toString()
                .substring(0, 8);
        // hash and the two "~" markers are all plain ASCII, so one byte each
        int budget = MAX_CONTEXT_LENGTH - hash.length() - 2;
        StringBuilder sb = new StringBuilder();
        int cost = 0;
        for (int i = 0; i < context.length(); ) {
            int cp = context.codePointAt(i);
            cost += jsonCost(cp);
            if (cost > budget) {
                break;
            }
            sb.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return sb.append('~').append(hash).append('~').toString();
    }

    /**
     * Returns the cached GHRepository, refreshing credentials if the cached
     * instance is older than {@link #CREDENTIAL_REFRESH_MILLIS}.
     */
    private GHRepository getRepository() {
        if (config != null && System.currentTimeMillis() - repositoryCreatedAt > CREDENTIAL_REFRESH_MILLIS) {
            refreshRepository();
        }
        return repository;
    }

    /**
     * Forces credential re-resolution and replaces the cached GHRepository.
     */
    private synchronized void refreshRepository() {
        if (config == null) {
            return;
        }
        GHRepository freshRepo = config.createRepository();
        if (freshRepo != null) {
            log(Level.INFO, "Successfully refreshed GitHub credentials");
            repository = freshRepo;
            repositoryCreatedAt = System.currentTimeMillis();
        } else {
            log(Level.WARNING, "Failed to refresh GitHub credentials, continuing with existing token");
        }
    }

    /**
     * Sends a notification when the job is complete
     * (currently no-op in this class).
     *
     * @param buildState state indicating success or failure
     * @param parameters build parameters
     */
    @Override
    public void notifyFinalBuildStatus(BuildStage.State buildState, Map<String, Object> parameters) {}

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
