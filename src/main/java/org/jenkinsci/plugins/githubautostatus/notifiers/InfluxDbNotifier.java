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
import org.apache.commons.lang.StringUtils;
import javax.annotation.Nullable;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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

        String data = String.format("stage,jobname=%s,owner=%s,repo=%s,branch=%s,stagename=%s,result=%s stagetime=%d,passed=%d",
                escapeTagValue(jobName),
                repoOwner,
                repoName,
                branchName,
                escapeTagValue(stageItem.getStageName()),
                result,
                timingInfo != null ? timingInfo : 0,
                passed);
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
        String jobName = (String) parameters.getOrDefault(BuildNotifierConstants.JOB_NAME, DEFAULT_STRING);
        int passed = buildState == BuildState.CompletedSuccess ? 1 : 0;
        long blockedDuration = getLong(parameters, BuildNotifierConstants.BLOCKED_DURATION);
        int blocked = blockedDuration > 0 ? 1 : 0;

        String dataPoint = String.format("job,jobname=%s,owner=%s,repo=%s,branch=%s,result=%s,blocked=%d jobtime=%d,blockedtime=%d,passed=%d",
                escapeTagValue(jobName),
                repoOwner,
                repoName,
                branchName,
                buildState.toString(),
                blocked,
                getLong(parameters, BuildNotifierConstants.JOB_DURATION) - blockedDuration,
                blockedDuration,
                passed);
        postData(dataPoint);

        notifyTestResults(jobName, (TestResults) parameters.get(BuildNotifierConstants.TEST_CASE_INFO));
        notifyCoverage(jobName, (CodeCoverage) parameters.get(BuildNotifierConstants.COVERAGE_INFO));
    }
    
    private long getLong(Map<String, Object> map, String mapKey) {
        Object mapValue = map.get(mapKey);
        
        if (mapValue != null) {
            return (Integer)mapValue;
        }
        return DEFAULT_LONG;
    }

    private void notifyCoverage(String jobName, @Nullable CodeCoverage coverageInfo) {
        if (coverageInfo != null) {
            String dataPoint = String.format("coverage,jobname=%s,owner=%s,repo=%s,branch=%s "
                    + "classes=%f,conditionals=%f,files=%f,lines=%f,methods=%f,packages=%f",
                    escapeTagValue(jobName),
                    repoOwner,
                    repoName,
                    branchName,
                    coverageInfo.getClasses(),
                    coverageInfo.getConditionals(),
                    coverageInfo.getFiles(),
                    coverageInfo.getLines(),
                    coverageInfo.getMethods(),
                    coverageInfo.getPackages());
            postData(dataPoint);
        }
    }

    private void notifyTestResults(String jobName, @Nullable TestResults testResults) {

        if (testResults != null) {
            String dataPoint = String.format("tests,jobname=%s,owner=%s,repo=%s,branch=%s passed=%d,skipped=%d,failed=%d",
                    escapeTagValue(jobName),
                    repoOwner,
                    repoName,
                    branchName,
                    testResults.getPassedTestCaseCount(),
                    testResults.getSkippedTestCaseCount(),
                    testResults.getFailedTestCaseCount());
            postData(dataPoint);

            for (TestSuite testSuite : testResults.getTestSuites()) {
                notifyTestSuite(jobName, testSuite);
            }
        }
    }

    private void notifyTestSuite(String jobName, TestSuite testSuite) {
        String suiteName = escapeTagValue(testSuite.getName());
        String dataPoint = String.format("testsuite,jobname=%s,owner=%s,repo=%s,branch=%s,suite=%s passed=%d,skipped=%d,failed=%d",
                escapeTagValue(jobName),
                repoOwner,
                repoName,
                branchName,
                suiteName,
                testSuite.getPassedTestCaseCount(),
                testSuite.getSkippedTestCaseCount(),
                testSuite.getFailedTestCaseCount());
        postData(dataPoint);

        for (TestCase testCase : testSuite.getTestCases()) {
            notifyTestCase(jobName, suiteName, testCase);
        }
    }

    private void notifyTestCase(String jobName, String suiteName, TestCase testCase) {
        String dataPoint = String.format("testcase,jobname=%s,owner=%s,repo=%s,branch=%s,suite=%s,testcase=%s passed=%d,skipped=%d,failed=%d",
                escapeTagValue(jobName),
                repoOwner,
                repoName,
                branchName,
                suiteName,
                escapeTagValue(testCase.getName()),
                testCase.getPassedCount(),
                testCase.getSkippedCount(),
                testCase.getFailedCount());
        postData(dataPoint);
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
