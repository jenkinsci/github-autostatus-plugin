/*
 * The MIT License
 *
 * Copyright 2019 nthienan.
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
import com.google.common.base.Strings;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hudson.model.Cause;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.config.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpNotifier extends BuildNotifier {

    protected String repoOwner;
    protected String repoName;
    protected String branchName;
    protected HttpNotifierConfig config;
    protected String authorization;
    protected Map<String, BuildStage> stageMap;
    protected Gson gson;

    public HttpNotifier(HttpNotifierConfig config) {
        if (null == config || Strings.isNullOrEmpty(config.getHttpEndpoint())) {
            return;
        }
        this.repoOwner = config.getRepoOwner();
        this.repoName = config.getRepoName();
        this.branchName = config.getBranchName();
        this.config = config;
        this.stageMap = new HashMap<>();
        UsernamePasswordCredentials credentials = config.getCredentials();
        if (credentials != null) {
            String username = credentials.getUsername();
            String password = credentials.getPassword().getPlainText();

            authorization = Base64.getEncoder().encodeToString(
                    String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8));
        }
        gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getAnnotation(SkipSerialisation.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
    }

    @Override
    public boolean isEnabled() {
        return (null != config && !Strings.isNullOrEmpty(config.getHttpEndpoint()));
    }

    @Override
    public void notifyBuildStageStatus(String jobName, BuildStage stageItem) {
        BuildStage.State buildState = stageItem.getBuildState();
        if (buildState == BuildStage.State.Pending) {
            return;
        }
        stageMap.put(stageItem.getStageName(), stageItem);
    }

    @Override
    public void notifyFinalBuildStatus(BuildStage.State buildState, Map<String, Object> parameters) {
        BuildStatus buildStatus = constructBuildStatus(buildState, parameters);
        TestResults testResults = (TestResults) parameters.get(BuildNotifierConstants.TEST_CASE_INFO);
        if (testResults != null) {
            buildStatus.setTestResult(testResults);
        }
        CodeCoverage coverage = (CodeCoverage) parameters.get(BuildNotifierConstants.COVERAGE_INFO);
        if (null != coverage) {
            buildStatus.setCoverage(coverage);
        }
        stageMap.forEach((name, stage) -> buildStatus.addStage(stage));

        log(Level.FINE, "Final build status: %s", gson.toJson(buildStatus));
        sendData(gson.toJson(buildStatus));
    }

    private BuildStatus constructBuildStatus(BuildStage.State buildState, Map<String, Object> parameters) {
        Run<?, ?> run = (Run<?, ?>) parameters.get(BuildNotifierConstants.BUILD_OBJECT);
        String jobName = (String) parameters.getOrDefault(BuildNotifierConstants.JOB_NAME, BuildNotifierConstants.DEFAULT_STRING);
        long blockedDuration = BuildNotifierConstants.getLong(parameters, BuildNotifierConstants.BLOCKED_DURATION);
        String buildUrl = run.getUrl();
        int buildNumber = run.getNumber();
        long buildDuration = BuildNotifierConstants.getLong(parameters, BuildNotifierConstants.JOB_DURATION) - blockedDuration;
        Cause cause = run.getCause(Cause.class);
        String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();
        BuildStatus result = new org.jenkinsci.plugins.githubautostatus.model.BuildStatus();
        result.setRepoOwner(repoOwner);
        result.setRepoName(repoName);
        result.setJobName(jobName);
        result.setBranch(branchName);
        result.setBuildUrl(buildUrl);
        result.setBuildNumber(buildNumber);
        result.setTrigger(buildCause);
        result.setBlocked(blockedDuration > 0);
        result.setBlockedTime(blockedDuration);
        result.setDuration(buildDuration);
        result.setPassed(buildState == BuildStage.State.CompletedSuccess);
        result.setResult(buildState);
        result.setTimestamp(Clock.system(TimeZone.getTimeZone("UTC").toZoneId()).millis() / 1000);
        return result;
    }

    private void sendData(String jsonData) {
        try (CloseableHttpClient httpclient = config.getHttpClient(!config.getHttpVerifySSL())) {
            HttpPost httppost = new HttpPost(config.getHttpEndpoint());

            httppost.setEntity(new StringEntity(jsonData,"UTF-8"));
            httppost.setHeader("Content-Type", "application/json");
            httppost.setHeader("Referer", Jenkins.get().getRootUrl());
            if (!Strings.isNullOrEmpty(authorization)) {
                httppost.setHeader("Authorization", String.format("Basic %s", authorization));
            }

            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode > 299) {
                    String statusLine = response.getStatusLine().toString();
                    log(Level.WARNING, "Could not send data to HTTP endpoint - %s", statusLine);
                    log(Level.WARNING, "HTTP endpoint - %s", config.getHttpEndpoint());
                    log(Level.WARNING, "Data - %s", jsonData);

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String reason = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                        log(Level.WARNING, "%s", reason);
                    }
                } else {
                    log(Level.INFO, "Successfully sent data to %s", config.getHttpEndpoint());
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
