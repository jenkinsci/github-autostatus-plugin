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
import com.google.common.base.Strings;
import hudson.model.Cause;
import hudson.model.Run;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.BuildStageModel;
import org.jenkinsci.plugins.githubautostatus.BuildState;
import org.jenkinsci.plugins.githubautostatus.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.CodeCoverage;
import org.jenkinsci.plugins.githubautostatus.model.TestCase;
import org.jenkinsci.plugins.githubautostatus.model.TestResults;
import org.jenkinsci.plugins.githubautostatus.model.TestSuite;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpNotifier extends BuildNotifier {

  protected String repoOwner;
  protected String repoName;
  protected String branchName;
  protected HttpNotifierConfig config;
  protected String authorization;
  protected Map<String, JSONObject> stageData;

  public static class BuildStatus {
    public static final String RepoOwner = "repoOwner";
    public static final String RepoName = "repoName";
    public static final String Result = "result";
    public static final String Branch = "branch";
    public static final String JobName = "jobName";
    public static final String JobTime = "jobTime";
    public static final String Blocked = "blocked";
    public static final String BlockedTime = "blockedTime";
    public static final String Passed = "passed";
    public static final String BuildUrl = "buildUrl";
    public static final String BuildNumber = "buildNumber";
    public static final String Trigger = "trigger";
    public static final String Stages = "stages";
    public static final String TestResults = "testResult";
    public static class TestResult {
      public static final String Passed = "passed";
      public static final String Skipped = "skipped";
      public static final String Failed = "failed";

      public static final String TestSuites = "suites";
      public static final String SuiteName = "suiteName";
      public static final String TestCases = "testCases";
      public static final String TestCaseName = "testCase";
    }

    public static final String CoverageResults = "coverage";
    public static class Coverage {
      public static final String Classes = "classes";
      public static final String Conditionals = "conditionals";
      public static final String Files = "files";
      public static final String Lines = "lines";
      public static final String Methods = "methods";
      public static final String Packages = "packages";
      public static final String Instructions = "instructions";
    }
  }

  public static class StageStatus {
    public static final String StageName = "stageName";
    public static final String Passed = "passed";
    public static final String Result = "result";
    public static final String StageTime = "stageTime";
  }

  public HttpNotifier(HttpNotifierConfig config) {
    if (null == config || Strings.isNullOrEmpty(config.getHttpEndpoint())) {
      return;
    }
    this.repoOwner = config.getRepoOwner();
    this.repoName = config.getRepoName();
    this.branchName = config.getBranchName();
    this.config = config;
    this.stageData = new HashMap<>();
    try {
      UsernamePasswordCredentials credentials = config.getCredentials();
      if (credentials != null) {
        String username = credentials.getUsername();
        String password = credentials.getPassword().getPlainText();

        authorization = Base64.getEncoder().encodeToString(
                String.format("%s:%s", username, password)
                        .getBytes("UTF-8"));
      }
    } catch (UnsupportedEncodingException ex) {
      Logger.getLogger(InfluxDbNotifier.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public boolean isEnabled() {
    return (null != config && !Strings.isNullOrEmpty(config.getHttpEndpoint()));
  }

  @Override
  public void notifyBuildStageStatus(String jobName, BuildStageModel stageItem) {
    BuildState buildState = stageItem.getBuildState();
    if (buildState == BuildState.Pending) {
      return;
    }

    Object timingInfo = stageItem.getEnvironment().get(BuildNotifierConstants.STAGE_DURATION);
    // Success and all Skipped stages are marked as successful
    int passed = buildState == BuildState.CompletedError ? 0 : 1;
    String result = buildState.toString();
    JSONObject data = new JSONObject();
    data.put(StageStatus.StageName, stageItem.getStageName());
    data.put(StageStatus.Passed, passed);
    data.put(StageStatus.Result, result);
    data.put(StageStatus.StageTime, timingInfo != null ? timingInfo : 0);

    stageData.put(stageItem.getStageName(), data);
  }

  @Override
  public void notifyFinalBuildStatus(BuildState buildState, Map<String, Object> parameters) {
    JSONObject buildStatus = new JSONObject();
    buildStatus.putAll(constructBuildStatus(buildState, parameters));
    buildStatus.put(BuildStatus.TestResults, constructTestResult((TestResults) parameters.get(BuildNotifierConstants.TEST_CASE_INFO)));
    buildStatus.put(BuildStatus.CoverageResults, constructCoverage((CodeCoverage) parameters.get(BuildNotifierConstants.COVERAGE_INFO)));

    JSONArray stages = new JSONArray();
    stageData.forEach((name, data) -> stages.add(data));
    buildStatus.put(BuildStatus.Stages, stages);

    log(Level.FINE, "Final build status: %s", buildStatus.toString());
    sendData(buildStatus.toString());
  }

  private JSONObject constructBuildStatus(BuildState buildState, Map<String, Object> parameters){
    Run<?, ?> run = (Run<?, ?>) parameters.get(BuildNotifierConstants.BUILD_OBJECT);
    String jobName = (String) parameters.getOrDefault(BuildNotifierConstants.JOB_NAME, BuildNotifierConstants.DEFAULT_STRING);
    int passed = buildState == BuildState.CompletedSuccess ? 1 : 0;
    long blockedDuration = BuildNotifierConstants.getLong(parameters, BuildNotifierConstants.BLOCKED_DURATION);
    int blocked = blockedDuration > 0 ? 1 : 0;
    String buildUrl = run.getUrl();
    int buildNumber = run.getNumber();
    long buildDuration = BuildNotifierConstants.getLong(parameters, BuildNotifierConstants.JOB_DURATION) - blockedDuration;
    Cause cause = run.getCause(Cause.class);
    String buildCause = cause == null ? BuildNotifierConstants.DEFAULT_STRING : cause.getShortDescription();

    JSONObject result = new JSONObject();
    result.put(BuildStatus.RepoOwner, repoOwner);
    result.put(BuildStatus.RepoName, repoName);
    result.put(BuildStatus.JobName, jobName);
    result.put(BuildStatus.Branch, branchName);
    result.put(BuildStatus.BuildUrl, buildUrl);
    result.put(BuildStatus.BuildNumber, buildNumber);
    result.put(BuildStatus.Trigger, buildCause);
    result.put(BuildStatus.Blocked, blocked);
    result.put(BuildStatus.BlockedTime, blockedDuration);
    result.put(BuildStatus.JobTime, buildDuration);
    result.put(BuildStatus.Passed, passed);
    result.put(BuildStatus.Result, buildState.toString());
    return result;
  }

  private JSONObject constructTestResult(@Nullable TestResults testResults) {
    JSONObject result = new JSONObject();
    if (testResults != null) {
      result.put(BuildStatus.TestResult.Passed, testResults.getPassedTestCaseCount());
      result.put(BuildStatus.TestResult.Skipped, testResults.getSkippedTestCaseCount());
      result.put(BuildStatus.TestResult.Failed, testResults.getFailedTestCaseCount());

      JSONArray suites = new JSONArray();
      testResults.getTestSuites().forEach(suite -> suites.add(constructTestSuiteData(suite)));
      result.put(BuildStatus.TestResult.TestSuites, suites);
    }
    return result;
  }

  private JSONObject constructTestSuiteData(TestSuite suite) {
    JSONObject result = new JSONObject();
    result.put(BuildStatus.TestResult.SuiteName, suite.getName());
    result.put(BuildStatus.TestResult.Passed, suite.getPassedTestCaseCount());
    result.put(BuildStatus.TestResult.Skipped, suite.getSkippedTestCaseCount());
    result.put(BuildStatus.TestResult.Failed, suite.getFailedTestCaseCount());

    JSONArray testCases = new JSONArray();
    suite.getTestCases().forEach(testCase -> testCases.add(constructTestCaseData(testCase)));
    result.put(BuildStatus.TestResult.TestCases, testCases);
    return result;
  }

  private JSONObject constructTestCaseData(TestCase testCase) {
    JSONObject result = new JSONObject();
    result.put(BuildStatus.TestResult.TestCaseName, testCase.getName());
    result.put(BuildStatus.TestResult.Passed, testCase.getPassedCount());
    result.put(BuildStatus.TestResult.Skipped, testCase.getSkippedCount());
    result.put(BuildStatus.TestResult.Failed, testCase.getFailedCount());
    return result;
  }

  private JSONObject constructCoverage(@Nullable CodeCoverage coverageInfo) {
    JSONObject result = new JSONObject();
    if (coverageInfo != null) {
      result.put(BuildStatus.Coverage.Classes, coverageInfo.getClasses());
      result.put(BuildStatus.Coverage.Conditionals, coverageInfo.getClasses());
      result.put(BuildStatus.Coverage.Methods, coverageInfo.getMethods());
      result.put(BuildStatus.Coverage.Files, coverageInfo.getFiles());
      result.put(BuildStatus.Coverage.Lines, coverageInfo.getLines());
      result.put(BuildStatus.Coverage.Packages, coverageInfo.getPackages());
      result.put(BuildStatus.Coverage.Instructions, coverageInfo.getInstructions());
    }
    return result;
  }

  private void sendData(String jsonData) {
    try (CloseableHttpClient httpclient = config.getHttpClient(!config.getHttpVerifySSL())) {
      HttpPost httppost = new HttpPost(config.getHttpEndpoint());

      httppost.setEntity(new StringEntity(jsonData));
      httppost.setHeader("Content-Type", "application/json");
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
            String reason = EntityUtils.toString(entity, "UTF-8");
            log(Level.WARNING, "%s", reason);
          }
        } else {
          log(Level.INFO, "Successfully sent data to %s", config.getHttpEndpoint());
        }
      }
    } catch (Exception ex) {
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
