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

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.model.Cause;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.config.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes job and stage measurements to an InfluxDB REST API.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class InfluxDbNotifier extends BuildNotifier {

    protected String repoOwner;
    protected String repoName;
    protected String branchName;
    protected String influxDbUrlString;
    protected InfluxDbNotifierConfig config;
    protected transient String authorization;

    /**
     * Constructor
     *
     * @param config InfluxDB configuration info
     */
    public InfluxDbNotifier(InfluxDbNotifierConfig config) {
        if (StringUtils.isEmpty(config.getInfluxDbUrlString()) || StringUtils.isEmpty(config.getInfluxDbDatabase())) {
            return;
        }
        String urlString = String.format("%s/write?db=%s", config.getInfluxDbUrlString(), config.getInfluxDbDatabase());
        try {
            UsernamePasswordCredentials credentials = config.getCredentials();
            if (credentials != null) {
                String influxDbUser = credentials.getUsername();
                String influxDbPassword = credentials.getPassword().getPlainText();

                authorization = Base64.getEncoder().encodeToString(
                        String.format("%s:%s", influxDbUser, influxDbPassword).getBytes(StandardCharsets.UTF_8));
            }
            if (!StringUtils.isEmpty(config.getInfluxDbRetentionPolicy())) {
                urlString = urlString.concat(
                        String.format("&rp=%s", URLEncoder.encode(config.getInfluxDbRetentionPolicy(), "UTF-8")));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(InfluxDbNotifier.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (config.influxDbIsReachable()) {
            this.repoOwner = config.getRepoOwner();
            this.repoName = config.getRepoName();
            this.branchName = config.getBranchName();
            if (StringUtils.isEmpty(this.repoOwner)) {
                this.repoOwner = BuildNotifierConstants.DEFAULT_STRING;
            }
            if (StringUtils.isEmpty(this.repoName)) {
                this.repoName = BuildNotifierConstants.DEFAULT_STRING;
            }
            if (StringUtils.isEmpty(this.branchName)) {
                this.branchName = BuildNotifierConstants.DEFAULT_STRING;
            }
            this.influxDbUrlString = urlString;
            this.config = config;
        }
    }


    /**
     * Determines whether this notifier is enabled.
     *
     * @return true if enabled; false otherwise
     */
    @Override
    public boolean isEnabled() {
        return this.config != null;
    }


    /**
     * Get whether the notifier wants to know about errors that happen outside of a stage.
     *
     * @return true; since this notifier reports these errors.
     */
    @Override
    public boolean wantsOutOfStageErrors() {
        return true;
    }

    /**
     * Sends a state change to InfluxDB.
     *
     * @param jobName the name of the job
     * @param stageItem stage item describing the new state
     */
    @Override
    public void notifyBuildStageStatus(String jobName, BuildStage stageItem) {
        if (stageItem.getBuildState() == BuildStage.State.Pending) {
            return;
        }

        String buildUrl = stageItem.getRun().getUrl();
        int buildNumber = stageItem.getRun().getNumber();
        Cause cause = stageItem.getRun().getCause(Cause.class);
        String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();

        String data = config.getSchema().formatStage(jobName,
                repoOwner,
                repoName,
                branchName,
                stageItem.getStageName(),
                stageItem.getBuildState().toString(),
                stageItem.getDuration(),
                stageItem.isPassed() ? 1 : 0,
                buildUrl,
                buildNumber,
                buildCause);

        postData(data);
    }

    /**
     * Sends the final build status to InfluxDB.
     *
     * @param buildState the new state
     * @param parameters build parameters
     */
    @Override
    public void notifyFinalBuildStatus(BuildStage.State buildState, Map<String, Object> parameters) {
        Run<?, ?> run = (Run<?, ?>) parameters.get(BuildNotifierConstants.BUILD_OBJECT);
        String jobName = (String) parameters.getOrDefault(BuildNotifierConstants.JOB_NAME, BuildNotifierConstants.DEFAULT_STRING);
        int passed = buildState == BuildStage.State.CompletedSuccess ? 1 : 0;
        long blockedDuration = BuildNotifierConstants.getLong(parameters, BuildNotifierConstants.BLOCKED_DURATION);
        int blocked = blockedDuration > 0 ? 1 : 0;
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        Cause cause = run.getCause(Cause.class);
        String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();

        String data = config.getSchema().formatJob(jobName,
                repoOwner,
                repoName,
                branchName,
                buildState.toString(),
                blocked,
                BuildNotifierConstants.getLong(parameters, BuildNotifierConstants.JOB_DURATION) - blockedDuration,
                blockedDuration,
                passed,
                buildUrl,
                buildNumber,
                buildCause);

        postData(data);

        if (!this.config.getIgnoreSendingTestResultsToInflux()) {
            notifyTestResults(jobName, (TestResults) parameters.get(BuildNotifierConstants.TEST_CASE_INFO), run);
        }
        if (!this.config.getIgnoreSendingTestCoverageToInflux()) {
            notifyCoverage(jobName, (CodeCoverage) parameters.get(BuildNotifierConstants.COVERAGE_INFO), run);
        }
    }

    private void notifyCoverage(String jobName, @Nullable CodeCoverage coverageInfo, Run<?, ?> run) {
        if (coverageInfo != null) {
            String buildUrl = run.getUrl();
            int buildNumber = run.getNumber();
            Cause cause = run.getCause(Cause.class);
            String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();

            String data = config.getSchema().formatCoverage(jobName,
                    repoOwner,
                    repoName,
                    branchName,
                    coverageInfo.getClasses(),
                    coverageInfo.getConditionals(),
                    coverageInfo.getFiles(),
                    coverageInfo.getLines(),
                    coverageInfo.getMethods(),
                    coverageInfo.getPackages(),
                    coverageInfo.getInstructions(),
                    buildUrl,
                    buildNumber,
                    buildCause);

            postData(data);
        }
    }

    private void notifyTestResults(String jobName, @Nullable TestResults testResults, Run<?, ?> run) {
        if (testResults != null) {
            String buildUrl = run.getUrl();
            int buildNumber = run.getNumber();
            Cause cause = run.getCause(Cause.class);
            String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();

            String data = config.getSchema().formatTests(jobName,
                    repoOwner,
                    repoName,
                    branchName,
                    testResults.getPassedTestCaseCount(),
                    testResults.getSkippedTestCaseCount(),
                    testResults.getFailedTestCaseCount(),
                    buildUrl,
                    buildNumber,
                    buildCause);

            postData(data);
	    
            for (TestSuite testSuite : testResults.getTestSuites()) {
                notifyTestSuite(jobName, testSuite, run);
            }
        }
    }

    private void notifyTestSuite(String jobName, TestSuite testSuite, Run<?, ?> run) {
        String suiteName = testSuite.getName();
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        Cause cause = run.getCause(Cause.class);
        String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();
	List<String> testSuiteQuery = new ArrayList<>();

        String data = config.getSchema().formatTestSuite(jobName,
                repoOwner,
                repoName,
                branchName,
                suiteName,
                testSuite.getDuration(),
                testSuite.getPassedTestCaseCount(),
                testSuite.getSkippedTestCaseCount(),
                testSuite.getFailedTestCaseCount(),
                buildUrl,
                buildNumber,
                buildCause);

	testSuiteQuery.add(data);
        for (TestCase testCase : testSuite.getTestCases()) {
            testSuiteQuery.add(notifyTestCase(jobName, suiteName, testCase, run));
        }
	postData(String.join("\\n", testSuiteQuery));
	
    }

    private String notifyTestCase(String jobName, String suiteName, TestCase testCase, Run<?, ?> run) {
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        Cause cause = run.getCause(Cause.class);
        String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();

        String data = config.getSchema().formatTestCase(jobName,
                repoOwner,
                repoName,
                branchName,
                suiteName,
                testCase.getName(),
                testCase.getPassedCount(),
                testCase.getSkippedCount(),
                testCase.getFailedCount(),
                buildUrl,
                buildNumber,
                buildCause);

	return data;
    }

    /**
     * Posts a new series data point.
     *
     * @param seriesInfo the data point
     */
    private synchronized void postData(String seriesInfo) {
        try (CloseableHttpClient httpclient = config.getHttpClient(false)) {
            HttpPost httppost = new HttpPost(influxDbUrlString);
            
            httppost.setEntity(new StringEntity(seriesInfo,"UTF-8"));

            if (!StringUtils.isEmpty(authorization)) {
                httppost.setHeader("Authorization", String.format("Basic %s", authorization));
            }

            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode > 299) {
                    String statusLine = response.getStatusLine().toString();
                    log(Level.WARNING, "Could not write to InfluxDB - %s", statusLine);
                    log(Level.WARNING, "InfluxDB URL - %s", influxDbUrlString);
                    log(Level.WARNING, "Series - %s", seriesInfo);

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String reason = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                        log(Level.WARNING, "%s", reason);
                    }

                }
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
            log(Level.SEVERE, ex);
        }
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
