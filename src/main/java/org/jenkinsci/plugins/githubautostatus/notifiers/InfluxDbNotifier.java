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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Cause;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.BuildStageModel;
import org.jenkinsci.plugins.githubautostatus.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.CodeCoverage;
import org.jenkinsci.plugins.githubautostatus.model.TestCase;
import org.jenkinsci.plugins.githubautostatus.model.TestResults;
import org.jenkinsci.plugins.githubautostatus.model.TestSuite;

/**
 * Writes job and stage measurements to an influxdb REST API.
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
public class InfluxDbNotifier extends BuildNotifier {

    protected final String DEFAULT_STRING = "none";
    protected final long DEFAULT_LONG = 0;
    protected String repoOwner;
    protected String repoName;
    protected String branchName;
    protected String influxDbUrlString;
    protected InfluxDbNotifierConfig config;
    protected transient String authorization;

    public static class SeriesNames {
        public static final String Coverage = "coverage";
        public static final String Stage = "stage";
        public static final String Job = "job";
        public static final String Tests = "tests";
        public static final String TestSuite = "testsuite";
        public static final String TestCase = "testcase";

    }
    public static class TagNames {
        public static final String Owner = "owner";
        public static final String Repo = "repo";
        public static final String Result = "result";
        public static final String StageName = "stagename";
        public static final String Suite = "suite";
    }
    public static class FieldNames {
        public static final String Blocked = "blocked";
        public static final String BlockedTime = "blockedtime";
        public static final String Branch = "branch";
        public static class Coverage {
            public static final String Conditionals = "conditionals";
            public static final String Classes = "classes";
            public static final String Files = "files";
            public static final String Lines = "lines";
            public static final String Methods = "methods";
            public static final String Packages = "packages";
        }
        public static class Test {
            public static final String Passed = "passed";
            public static final String Skipped = "skipped";
            public static final String Failed = "failed";
        }
        public static class TestSuite {
            public static final String Suite = "suite";
        }
        public static class TestCase {
            public static final String TestCase = "testcase";
        }
        public static final String JobName = "jobname";
        public static final String JobTime = "jobtime";
        public static final String Passed = "passed";
        public static final String StageTime = "stagetime";
        public static final String BuildUrl = "buildurl";
        public static final String BuildNumber = "buildnumber";
        public static final String Trigger = "trigger";
    }


    /**
     * Constructor
     *
     * @param config influxdb configuration info
     */
    public InfluxDbNotifier(
            InfluxDbNotifierConfig config) {
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
                        String.format("%s:%s", 
                                influxDbUser, 
                                influxDbPassword).getBytes("UTF-8"));
            }
            if (!StringUtils.isEmpty(config.getInfluxDbRetentionPolicy())) {
                urlString = urlString.concat(
                        String.format("&rp=%s", 
                                URLEncoder.encode(config.getInfluxDbRetentionPolicy(), "UTF-8")));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(InfluxDbNotifier.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (config.influxDbIsReachable()) {
            this.repoOwner = escapeTagValue(config.getRepoOwner());
            this.repoName = escapeTagValue(config.getRepoName());
            this.branchName = escapeTagValue(config.getBranchName());
            if (StringUtils.isEmpty(this.repoOwner)) {
                this.repoOwner = DEFAULT_STRING;
            }
            if (StringUtils.isEmpty(this.repoName)) {
                this.repoName = DEFAULT_STRING;
            }
            if (StringUtils.isEmpty(this.branchName)) {
                this.branchName = DEFAULT_STRING;
            }
            this.influxDbUrlString = urlString;
            this.config = config;
        }
    }

    /**
     * Determine whether notifier is enabled
     *
     * @return true if enabled; false otherwise
     */
    @Override
    public boolean isEnabled() {
        return this.config != null;
    }

    /**
     * Send a state change to influx
     *
     * @param jobName the name of the job
     * @param stageItem stage item describing the new state
     */
    @Override
    public void notifyBuildStageStatus(String jobName, BuildStageModel stageItem) {

        BuildState buildState = stageItem.getBuildState();

        Object timingInfo = stageItem.getEnvironment().get(BuildNotifierConstants.STAGE_DURATION);
        if (buildState == BuildState.Pending) {
            return;
        }

        // Success and all Skipped stages are marked as successful
        int passed = buildState == BuildState.CompletedError ? 0 : 1;
        String result = buildState.toString();
        String buildUrl = stageItem.getRun().getUrl();
        int buildNumber = stageItem.getRun().getNumber();
        String buildCause = stageItem.getRun().getCause(Cause.class).getShortDescription();
        String data = new StringBuilder(SeriesNames.Stage)
                // Tags
                .append(String.format(",%s=%s", TagNames.Owner, repoOwner))
                .append(String.format(",%s=%s", TagNames.Repo, repoName))
                .append(String.format(",%s=\"%s\"", TagNames.StageName, escapeTagValue(stageItem.getStageName())))
                .append(String.format(",%s=%s", TagNames.Result, result))
                // Fields
                .append(String.format(" %s=\"%s\"", FieldNames.JobName, escapeTagValue(jobName)))
                .append(String.format(",%s=\"%s\"", FieldNames.Branch, branchName))
                .append(String.format(",%s=%d", FieldNames.StageTime, timingInfo != null ? timingInfo : 0))
                .append(String.format(",%s=%d", FieldNames.Passed, passed))
                .append(String.format(",%s=%d", FieldNames.BuildNumber, buildNumber))
                .append(String.format(",%s=\"%s\"", FieldNames.BuildUrl, escapeTagValue(buildUrl)))
                .append(String.format(",%s=\"%s\"", FieldNames.Trigger, buildCause))
                .toString();

        postData(data);
    }

    /**
     * Send final build status to influx
     *
     * @param buildState the new state
     * @param parameters build parameters
     */
    @Override
    public void notifyFinalBuildStatus(BuildState buildState, Map<String, Object> parameters) {
        Run<?, ?> run = (Run<?, ?>) parameters.get(BuildNotifierConstants.BUILD_OBJECT);
        String jobName = (String) parameters.getOrDefault(BuildNotifierConstants.JOB_NAME, DEFAULT_STRING);
        int passed = buildState == BuildState.CompletedSuccess ? 1 : 0;
        long blockedDuration = getLong(parameters, BuildNotifierConstants.BLOCKED_DURATION);
        int blocked = blockedDuration > 0 ? 1 : 0;
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        String buildCause = run.getCause(Cause.class).getShortDescription();

        String data = new StringBuilder(SeriesNames.Job)
                // Tags
                .append(String.format(",%s=%s", TagNames.Owner, repoOwner))
                .append(String.format(",%s=%s", TagNames.Repo, repoName))
                .append(String.format(",%s=%s", TagNames.Result, buildState.toString()))
                // Fields
                .append(String.format(" %s=\"%s\"", FieldNames.JobName, escapeTagValue(jobName)))
                .append(String.format(",%s=\"%s\"", FieldNames.Branch, branchName))
                .append(String.format(",%s=%d", FieldNames.JobTime, getLong(parameters, BuildNotifierConstants.JOB_DURATION) - blockedDuration))
                .append(String.format(",%s=%d", FieldNames.Blocked, blocked))
                .append(String.format(",%s=%d", FieldNames.BlockedTime, blockedDuration))
                .append(String.format(",%s=%d", FieldNames.Passed, passed))
                .append(String.format(",%s=\"%s\"", FieldNames.BuildUrl, escapeTagValue(buildUrl)))
                .append(String.format(",%s=%d", FieldNames.BuildNumber, buildNumber))
                .append(String.format(",%s=\"%s\"", FieldNames.Trigger, buildCause))
                .toString();

        postData(data);

        notifyTestResults(jobName, (TestResults) parameters.get(BuildNotifierConstants.TEST_CASE_INFO), run);
        notifyCoverage(jobName, (CodeCoverage) parameters.get(BuildNotifierConstants.COVERAGE_INFO), run);
    }
    
    private long getLong(Map<String, Object> map, String mapKey) {
        Object mapValue = map.get(mapKey);
        
        if (mapValue != null) {
            return (long)mapValue;
        }
        return DEFAULT_LONG;
    }

    private void notifyCoverage(String jobName, @Nullable CodeCoverage coverageInfo, Run<?, ?> run) {
        if (coverageInfo != null) {
            String buildUrl = run.getUrl();
            int buildNumber = run.getNumber();
            String buildCause = run.getCause(Cause.class).getShortDescription();

            String data = new StringBuilder(SeriesNames.Coverage)
                    // Tags
                    .append(String.format(",%s=%s", TagNames.Owner, repoOwner))
                    .append(String.format(",%s=%s", TagNames.Repo, repoName))

                    // Fields
                    .append(String.format(" %s=\"%s\"", FieldNames.JobName, escapeTagValue(jobName)))
                    .append(String.format(",%s=\"%s\"", FieldNames.Branch, branchName))
                    .append(String.format(",%s=%f", FieldNames.Coverage.Classes, coverageInfo.getClasses()))
                    .append(String.format(",%s=%f", FieldNames.Coverage.Conditionals, coverageInfo.getConditionals()))
                    .append(String.format(",%s=%f", FieldNames.Coverage.Files, coverageInfo.getFiles()))
                    .append(String.format(",%s=%f", FieldNames.Coverage.Lines, coverageInfo.getLines()))
                    .append(String.format(",%s=%f", FieldNames.Coverage.Methods, coverageInfo.getMethods()))
                    .append(String.format(",%s=%f", FieldNames.Coverage.Packages, coverageInfo.getPackages()))
                    .append(String.format(",%s=\"%s\"", FieldNames.BuildUrl, escapeTagValue(buildUrl)))
                    .append(String.format(",%s=%d", FieldNames.BuildNumber, buildNumber))
                    .append(String.format(",%s=\"%s\"", FieldNames.Trigger, buildCause))
                    .toString();

            postData(data);
        }
    }

    private void notifyTestResults(String jobName, @Nullable TestResults testResults, Run<?, ?> run) {

        if (testResults != null) {
            String buildUrl = run.getUrl();
            int buildNumber = run.getNumber();
            String buildCause = run.getCause(Cause.class).getShortDescription();
            String data = new StringBuilder(SeriesNames.Tests)
                    // Tags
                    .append(String.format(",%s=%s", TagNames.Owner, repoOwner))
                    .append(String.format(",%s=%s", TagNames.Repo, repoName))

                    // Fields
                    .append(String.format(" %s=\"%s\"", FieldNames.Branch, branchName))
                    .append(String.format(",%s=\"%s\"", FieldNames.JobName, escapeTagValue(jobName)))
                    .append(String.format(",%s=\"%s\"", FieldNames.BuildUrl, escapeTagValue(buildUrl)))
                    .append(String.format(",%s=%d", FieldNames.BuildNumber, buildNumber))
                    .append(String.format(",%s=\"%s\"", FieldNames.Trigger, buildCause))
                    .append(String.format(",%s=%d", FieldNames.Test.Passed, testResults.getPassedTestCaseCount()))
                    .append(String.format(",%s=%d", FieldNames.Test.Skipped, testResults.getSkippedTestCaseCount()))
                    .append(String.format(",%s=%d", FieldNames.Test.Failed, testResults.getFailedTestCaseCount()))
                    .toString();

            postData(data);

            for (TestSuite testSuite : testResults.getTestSuites()) {
                notifyTestSuite(jobName, testSuite, run);
            }
        }
    }

    private void notifyTestSuite(String jobName, TestSuite testSuite, Run<?, ?> run) {
        String suiteName = escapeTagValue(testSuite.getName());
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        String buildCause = run.getCause(Cause.class).getShortDescription();
        String data = new StringBuilder(SeriesNames.TestSuite)
                // Tags
                .append(String.format(",%s=%s", TagNames.Owner, repoOwner))
                .append(String.format(",%s=%s", TagNames.Repo, repoName))

                // Fields
                .append(String.format(" %s=\"%s\"", FieldNames.Branch, branchName))
                .append(String.format(",%s=\"%s\"", FieldNames.BuildUrl, escapeTagValue(buildUrl)))
                .append(String.format(",%s=%d", FieldNames.BuildNumber, buildNumber))
                .append(String.format(",%s=\"%s\"", FieldNames.Trigger, buildCause))
                .append(String.format(",%s=\"%s\"", FieldNames.TestSuite.Suite, escapeTagValue(suiteName)))
                .append(String.format(",%s=\"%s\"", FieldNames.JobName, escapeTagValue(jobName)))
                .append(String.format(",%s=%d", FieldNames.Test.Passed, testSuite.getPassedTestCaseCount()))
                .append(String.format(",%s=%d", FieldNames.Test.Skipped, testSuite.getSkippedTestCaseCount()))
                .append(String.format(",%s=%d", FieldNames.Test.Failed, testSuite.getFailedTestCaseCount()))
                .toString();

        postData(data);

        for (TestCase testCase : testSuite.getTestCases()) {
            notifyTestCase(jobName, suiteName, testCase, run);
        }
    }

    private void notifyTestCase(String jobName, String suiteName, TestCase testCase, Run<?, ?> run) {
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        String buildCause = run.getCause(Cause.class).getShortDescription();

        String data = new StringBuilder(SeriesNames.TestCase)
                // Tags
                .append(String.format(",%s=%s", TagNames.Owner, repoOwner))
                .append(String.format(",%s=%s", TagNames.Repo, repoName))
                .append(String.format(",%s=%s", TagNames.Suite, escapeTagValue(suiteName)))

                // Fields
                .append(String.format(" %s=\"%s\"", FieldNames.Branch, branchName))
                .append(String.format(" %s=\"%s\"", FieldNames.TestCase.TestCase, escapeTagValue(testCase.getName())))
                .append(String.format(",%s=\"%s\"", FieldNames.BuildUrl, escapeTagValue(buildUrl)))
                .append(String.format(",%s=%d", FieldNames.BuildNumber, buildNumber))
                .append(String.format(",%s=\"%s\"", FieldNames.Trigger, buildCause))
                .append(String.format(",%s=\"%s\"", FieldNames.JobName, escapeTagValue(jobName)))
                .append(String.format(",%s=%d", FieldNames.Test.Passed, testCase.getPassedCount()))
                .append(String.format(",%s=%d", FieldNames.Test.Skipped, testCase.getSkippedCount()))
                .append(String.format(",%s=%d", FieldNames.Test.Failed, testCase.getFailedCount()))
                .toString();
        postData(data);
    }

    protected String escapeTagValue(String tagValue) {
        return tagValue.replace(" ", "\\ ")
                .replace(",", "\\,")
                .replace("=", "\\=");
    }

    /**
     * Posts a new series data point
     *
     * @param seriesInfo the data point
     */
    private void postData(String seriesInfo) {
        try (CloseableHttpClient httpclient = config.getHttpClient()) {
            HttpPost httppost = new HttpPost(influxDbUrlString);

            httppost.setEntity(new StringEntity(seriesInfo));
            
            if (!StringUtils.isEmpty(authorization)) {
                httppost.setHeader("Authorization", String.format("Basic %s", authorization));
            }

            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode > 299) {
                    String statusLine = response.getStatusLine().toString();
                    log(Level.WARNING, "Could not write to influxdb - %s", statusLine);
                    log(Level.WARNING, "Influxdb url - %s", influxDbUrlString);
                    log(Level.WARNING, "Series - %s", seriesInfo);

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String reason = EntityUtils.toString(entity, "UTF-8");
                        log(Level.WARNING, "%s", reason);
                    }

                }
            }
        } catch (IOException ex) {
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
