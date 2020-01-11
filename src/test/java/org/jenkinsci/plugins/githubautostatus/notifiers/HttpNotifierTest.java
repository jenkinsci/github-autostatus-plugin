package org.jenkinsci.plugins.githubautostatus.notifiers;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.config.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author nthienan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class})
public class HttpNotifierTest {

  private HttpNotifierConfig mockConfig;

  private String username = "user-a";
  private String password = "mock-password";
  private String credentialId = "credential-id";

  private HttpNotifier notifier;
  private Map<String, BuildStage> mockStageMap;
  private CloseableHttpClient mockHttpClient;
  private StatusLine mockStatusLine;
  private Run<?, ?> mockRun;
  private String bodyData;
  private Map<String, String> requestHeaders = new HashMap<>();

  private String repoOwner = "mock-repo-name";
  private String repoName = "mock-repo-name";
  private String jobName = "mock-job";
  private long jobDuration = 1234L;
  private String trigger = "started by user A";
  private String buildUrl = "https://jenkins.com/job/job-1/build/1";
  private final String jenkinsUrl = "https://mock-jenkins.com:8443/";
  private int buildNumber = 123123;

  @Before
  public void setUp() throws Exception {
    mockConfig = mock(HttpNotifierConfig.class);

    when(mockConfig.getHttpEndpoint()).thenReturn("https://mock.com/jenkins");
    when(mockConfig.getRepoName()).thenReturn(repoName);
    when(mockConfig.getRepoOwner()).thenReturn(repoOwner);
    when(mockConfig.getBranchName()).thenReturn("");
    when(mockConfig.getHttpCredentialsId()).thenReturn(credentialId);
    UsernamePasswordCredentials credentials = new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL, credentialId, "description", username, password);
    when(mockConfig.getCredentials()).thenReturn(credentials);
    when(mockConfig.getHttpVerifySSL()).thenReturn(true);

    mockHttpClient = mock(CloseableHttpClient.class);
    when(mockConfig.getHttpClient(false)).thenReturn(mockHttpClient);

    CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
    mockStatusLine = mock(StatusLine.class);
    when(mockStatusLine.getStatusCode()).thenReturn(200);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);

    when(mockHttpClient.execute(any())).thenAnswer((InvocationOnMock invocation) -> {
      HttpPost httpPost = (HttpPost) invocation.getArguments()[0];
      Header[] headers = httpPost.getAllHeaders();
      for (Header header : headers) {
        requestHeaders.put(header.getName(), header.getValue());
      }
      HttpEntity entity = httpPost.getEntity();
      bodyData = EntityUtils.toString(entity);
      return mockResponse;
    });

    Cause mockCause = mock(Cause.UserIdCause.UserIdCause.class);
    when(mockCause.getShortDescription()).thenReturn(trigger);

    mockRun = mock(AbstractBuild.class);
    when(mockRun.getCause(Cause.class)).thenReturn(mockCause);
    when(mockRun.getNumber()).thenReturn(buildNumber);
    when(mockRun.getUrl()).thenReturn(buildUrl);

    mockStageMap = mock(HashMap.class);
    notifier = new HttpNotifier(mockConfig);
    notifier.stageMap = mockStageMap;

    Jenkins jenkins = mock(Jenkins.class);
    when(jenkins.getRootUrl()).thenReturn(jenkinsUrl);
    PowerMockito.mockStatic(Jenkins.class);
    when(Jenkins.get()).thenReturn(jenkins);
  }

  @Test
  public void testIsEnabledTrue() {
    assertTrue(notifier.isEnabled());
  }

  @Test
  public void testIsEnabledFalse() {
    when(mockConfig.getHttpEndpoint()).thenReturn("");
    assertFalse(notifier.isEnabled());
  }

  @Test
  public void testBasicAuth() {
    String expectedResult = String.format("%s:%s", username, password);
    String actualResult = new String(Base64.getDecoder().decode(notifier.authorization));

    assertEquals(expectedResult, actualResult);
  }

  @Test
  public void testBasicAuthNull() {
    when(mockConfig.getCredentials()).thenReturn(null);
    notifier = new HttpNotifier(mockConfig);
    assertNull(notifier.authorization);
  }

  @Test
  public void testNotifyBuildStageStatusPending() {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.Pending);
    notifier.notifyBuildStageStatus(stageName, stage);
    verify(mockStageMap, times(0)).put(any(), any());
  }

  @Test
  public void testNotifyBuildStageStatusSkippedConditional() {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.SkippedConditional);
    notifier.notifyBuildStageStatus(stageName, stage);
    verify(mockStageMap, times(1)).put(eq(stageName), any(BuildStage.class));
  }

  @Test
  public void testNotifyBuildStageStatusSkippedUnstable() {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.SkippedUnstable);
    notifier.notifyBuildStageStatus(stageName, stage);
    verify(mockStageMap, times(1)).put(eq(stageName), any(BuildStage.class));
  }

  @Test
  public void testNotifyBuildStageStatusSkippedFailure() {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.SkippedFailure);
    notifier.notifyBuildStageStatus(stageName, stage);
    verify(mockStageMap, times(1)).put(eq(stageName), any(BuildStage.class));
  }

  @Test
  public void testNotifyBuildStageStatusCompletedSuccess() {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.CompletedSuccess);
    notifier.notifyBuildStageStatus(stageName, stage);
    verify(mockStageMap, times(1)).put(eq(stageName), any(BuildStage.class));
  }

  @Test
  public void testNotifyBuildStageStatusCompletedError() {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.CompletedError);
    notifier.notifyBuildStageStatus(stageName, stage);
    verify(mockStageMap, times(1)).put(eq(stageName), any(BuildStage.class));
  }

  @Test
  public void testNotifyFinalBuildStatusNoTestResults() throws IOException {
    String stageName = "stage-1";
    BuildStage stage = new BuildStage(stageName, new HashMap<>(), BuildStage.State.CompletedSuccess);
    notifier.stageMap = new HashMap<>();
    notifier.notifyBuildStageStatus(stageName, stage);
    notifier.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, createParameter());
    BuildStatus actualResult = notifier.gson.fromJson(bodyData, BuildStatus.class);
    assertCommonBuildStatusProperties(actualResult);
    assertEquals(BuildStage.State.CompletedSuccess, actualResult.getResult());
    assertTrue(actualResult.isPassed());
    assertEquals(1, actualResult.getStages().size());
    assertNull(actualResult.getTestResult());
    verify(mockHttpClient).execute(any());
  }

  @Test
  public void testNotifyFinalBuildStatusWithTestResults() throws IOException {
    TestResults testResults = notifier.gson.fromJson("{\"passed\":2,\"skipped\":0,\"failed\":0,\"suites\":[{\"name\":\"com.devops.test.DevOpsJavaSampleApplicationTests\",\"testCases\":[{\"name\":\"com.devops.test.DevOpsJavaSampleApplicationTests.testGreeting\",\"failed\":false,\"passed\":true,\"skipped\":false,\"result\":\"Passed\"},{\"name\":\"com.devops.test.DevOpsJavaSampleApplicationTests.contextLoads\",\"failed\":false,\"passed\":true,\"skipped\":false,\"result\":\"Passed\"}],\"passed\":2,\"skipped\":0,\"failed\":0}]}",
            TestResults.class);
    Map<String, Object> parameter = createParameter();
    parameter.put(BuildNotifierConstants.TEST_CASE_INFO, testResults);
    notifier.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, parameter);
    BuildStatus actualResult = notifier.gson.fromJson(bodyData, BuildStatus.class);

    assertCommonBuildStatusProperties(actualResult);
    assertEquals(BuildStage.State.CompletedSuccess, actualResult.getResult());
    assertTrue(actualResult.isPassed());
    assertEquals(0, actualResult.getStages().size());
    assertEquals(testResults, actualResult.getTestResult());
    verify(mockHttpClient).execute(any());
  }

  @Test
  public void testNotifyFinalBuildStatusWithCoverage() throws IOException {
    CodeCoverage coverage = notifier.gson.fromJson("{\"conditionals\":100.0,\"classes\":100.0,\"files\":-1.0,\"lines\":50.0,\"methods\":0.0,\"packages\":-1.0,\"instructions\":50.0}",
            CodeCoverage.class);
    Map<String, Object> parameter = createParameter();
    parameter.put(BuildNotifierConstants.COVERAGE_INFO, coverage);
    notifier.notifyFinalBuildStatus(BuildStage.State.CompletedError, parameter);
    BuildStatus actualResult = notifier.gson.fromJson(bodyData, BuildStatus.class);

    assertCommonBuildStatusProperties(actualResult);
    assertEquals(BuildStage.State.CompletedError, actualResult.getResult());
    assertFalse(actualResult.isPassed());
    assertEquals(0, actualResult.getStages().size());
    assertEquals(coverage, actualResult.getCoverage());
    verify(mockHttpClient).execute(any());
  }

  @Test
  public void testNotifyFinalBuildStatusError() throws IOException {
    when(mockStatusLine.getStatusCode()).thenReturn(400);
    notifier.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, createParameter());
    BuildStatus actualResult = notifier.gson.fromJson(bodyData, BuildStatus.class);
    assertCommonBuildStatusProperties(actualResult);
    assertEquals(BuildStage.State.CompletedSuccess, actualResult.getResult());
    assertTrue(actualResult.isPassed());
    assertEquals(0, actualResult.getStages().size());
    assertNull(actualResult.getTestResult());
    verify(mockHttpClient).execute(any());
  }

  @Test
  public void testNotifyFinalBuildStatusIoException() throws IOException {
    when(mockStatusLine.getStatusCode()).thenAnswer((InvocationOnMock invocation) -> {
      throw new IOException();
    });
    notifier.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, createParameter());
    BuildStatus actualResult = notifier.gson.fromJson(bodyData, BuildStatus.class);
    assertCommonBuildStatusProperties(actualResult);
    assertEquals(BuildStage.State.CompletedSuccess, actualResult.getResult());
    assertTrue(actualResult.isPassed());
    assertEquals(0, actualResult.getStages().size());
    assertNull(actualResult.getTestResult());
    verify(mockHttpClient).execute(any());
  }

  private void assertCommonBuildStatusProperties(BuildStatus actualResult) {
    assertEquals(repoOwner, actualResult.getRepoOwner());
    assertEquals(repoName, actualResult.getRepoName());
    assertEquals(jobName, actualResult.getJobName());
    assertNotEquals(0, actualResult.getTimestamp());
    assertFalse(actualResult.isBlocked());
    assertEquals(trigger, actualResult.getTrigger());
    assertEquals(buildUrl, actualResult.getBuildUrl());
    assertEquals(buildNumber, actualResult.getBuildNumber());
    assertEquals(jenkinsUrl, requestHeaders.get("Referer"));
  }

  private Map<String, Object> createParameter() {
    Map<String, Object> parameter = new HashMap<>();
    parameter.put(BuildNotifierConstants.BUILD_OBJECT, mockRun);
    parameter.put(BuildNotifierConstants.JOB_NAME, jobName);
    parameter.put(BuildNotifierConstants.JOB_DURATION, jobDuration);
    parameter.put(BuildNotifierConstants.BLOCKED_DURATION, 0L);
    return parameter;
  }
}
