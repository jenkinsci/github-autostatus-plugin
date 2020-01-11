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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.model.Cause;

import hudson.model.Run;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.model.BuildState;
import org.jenkinsci.plugins.githubautostatus.config.InfluxDbNotifierConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;

/**
 *
 * @author Jeff Pearce (github jeffpearce)
 */
public class InfluxDbNotifierTest {

    private InfluxDbNotifierConfig config;
    private final String influxDbCredentialsId = "mock-credentials";
    private final String influxDbUser = "mock-user";
    private final String influxDbPassword = "mock-password";
    private String statusLine;
    private CloseableHttpClient mockHttpClient;
    private StatusLine mockStatusLine;
    private Run<?,?> mockRun;
    private Cause mockCause;

    public InfluxDbNotifierTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        statusLine = null;
        config = mock(InfluxDbNotifierConfig.class);
        when(config.influxDbIsReachable()).thenReturn(true);
        when(config.getInfluxDbUrlString()).thenReturn("http://fake");
        when(config.getInfluxDbDatabase()).thenReturn("mockdb");
        when(config.getRepoOwner()).thenReturn("mockowner");
        when(config.getRepoName()).thenReturn("mockrepo");
        when(config.getBranchName()).thenReturn("mockbranch");
        when(config.getSchema()).thenReturn(new InfluxDbNotifierSchemas.SchemaInfo.V2());

        mockHttpClient = mock(CloseableHttpClient.class);
        when(config.getHttpClient(false)).thenReturn(mockHttpClient);

        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);

        when(mockHttpClient.execute(any())).thenAnswer((InvocationOnMock invocation) -> {
            Object[] args = invocation.getArguments();
            Object mock = invocation.getMock();
            HttpEntity entity = ((HttpPost) args[0]).getEntity();
            statusLine = EntityUtils.toString(entity);
            return mockResponse;
        });

        mockCause = mock(Cause.UserIdCause.UserIdCause.class);
        when(mockCause.getShortDescription()).thenReturn("user A");

        mockRun = mock(AbstractBuild.class);
        when(mockRun.getCause(Cause.class)).thenReturn(mockCause);
        when(mockRun.getNumber()).thenReturn(1);
        when(mockRun.getUrl()).thenReturn("https://jenkins.com/1");
    }


    /**
     * Test of isEnabled method, of class InfluxDbNotifier.
     */
    @Test
    public void testDisabledEmptyConfig() {
        when(config.getInfluxDbDatabase()).thenReturn("");
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertFalse(instance.isEnabled());
    }

    @Test
    public void testIsEnabled() {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertTrue(instance.isEnabled());
    }

    @Test
    public void testIsDisabledUrl() {
        when(config.influxDbIsReachable()).thenReturn(false);
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertFalse(instance.isEnabled());
    }

    @Test
    public void testUrl() {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertEquals("http://fake/write?db=mockdb", instance.influxDbUrlString);
    }

    @Test
    public void testBasicAuth() {
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                        influxDbCredentialsId,
                        "Description",
                        influxDbUser,
                        influxDbPassword);
        when(config.getCredentials())
                .thenReturn(credentials);

        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertEquals("http://fake/write?db=mockdb",
                instance.influxDbUrlString);
        assertEquals(new String(Base64.getDecoder().decode(instance.authorization)),
                "mock-user:mock-password");
    }

    @Test
    public void testUrlRetention() {
        when(config.getInfluxDbRetentionPolicy()).thenReturn("mockretention");
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertEquals("http://fake/write?db=mockdb&rp=mockretention", instance.influxDbUrlString);
    }

    @Test
    public void TestDefaultsEmptyString() {
        when(config.getBranchName()).thenReturn("");
        when(config.getRepoOwner()).thenReturn("");
        when(config.getRepoName()).thenReturn("");

        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, instance.repoOwner);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, instance.repoName);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, instance.branchName);
    }

    @Test
    public void TestDefaultNull() {
        when(config.getBranchName()).thenReturn(null);
        when(config.getRepoOwner()).thenReturn(null);
        when(config.getRepoName()).thenReturn(null);

        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, instance.repoOwner);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, instance.repoName);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, instance.branchName);
    }

    @Test
    public void testNotifyBuildStageStatus() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        BuildStage stageItem = new BuildStage("mocknodename");
        stageItem.setBuildState(BuildStage.State.CompletedSuccess);
        stageItem.setRun(mockRun);
        instance.notifyBuildStageStatus("mockjobname", stageItem);

        verify(mockHttpClient).execute(any());
        assertEquals(
                "stage,owner=mockowner,repo=mockrepo,stagename=mocknodename,result=CompletedSuccess jobname=\"mockjobname\",branch=\"mockbranch\",stagetime=0,passed=1,buildurl=\"https://jenkins.com/1\",buildnumber=1,trigger=\"user A\"",
                statusLine);
    }

    @Test
    public void testNotifyBuildStageStatusHttpError() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        when(mockStatusLine.getStatusCode()).thenReturn(400);

        BuildStage stageItem = new BuildStage("mocknodename");
        stageItem.setBuildState(BuildStage.State.CompletedSuccess);
        stageItem.setRun(mockRun);
        instance.notifyBuildStageStatus("mockjobname", stageItem);

        verify(mockHttpClient).execute(any());
        assertEquals(
                "stage,owner=mockowner,repo=mockrepo,stagename=mocknodename,result=CompletedSuccess jobname=\"mockjobname\",branch=\"mockbranch\",stagetime=0,passed=1,buildurl=\"https://jenkins.com/1\",buildnumber=1,trigger=\"user A\"",
                statusLine);
    }

    @Test
    public void testNotifyBuildStageStatusIoException() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        when(mockStatusLine.getStatusCode()).thenAnswer((InvocationOnMock invocation) -> {
            throw new IOException();
        });

        BuildStage stageItem = new BuildStage("mocknodename");
        stageItem.setBuildState(BuildStage.State.CompletedSuccess);
        stageItem.setRun(mockRun);
        instance.notifyBuildStageStatus("mockjobname", stageItem);

        verify(mockHttpClient).execute(any());
        assertEquals("stage,owner=mockowner,repo=mockrepo,stagename=mocknodename,result=CompletedSuccess jobname=\"mockjobname\",branch=\"mockbranch\",stagetime=0,passed=1,buildurl=\"https://jenkins.com/1\",buildnumber=1,trigger=\"user A\"",
        statusLine);

    }

    @Test
    public void testNotifyBuildStageStatusPending() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        BuildStage stageItem = new BuildStage("mocknodename");

        instance.notifyBuildStageStatus("mockjobname", stageItem);

        verify(mockHttpClient, never()).execute(any());
        assertNull(statusLine);
    }

    @Test
    public void testNotifyFinalBuildStateSuccess() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        HashMap<String, Object> jobParams = new HashMap<String, Object>();
        jobParams.put(BuildNotifierConstants.JOB_NAME, "mockjobname");
        jobParams.put(BuildNotifierConstants.JOB_DURATION, 88L);
        jobParams.put(BuildNotifierConstants.BLOCKED_DURATION, 12L);
        jobParams.put(BuildNotifierConstants.BUILD_OBJECT, mockRun);
        instance.notifyFinalBuildStatus(BuildStage.State.CompletedSuccess, jobParams);

        verify(mockHttpClient).execute(any());
        assertEquals(
                "job,owner=mockowner,repo=mockrepo,result=CompletedSuccess jobname=\"mockjobname\",branch=\"mockbranch\",blocked=1,jobtime=76,blockedtime=12,passed=1,buildurl=\"https://jenkins.com/1\",buildnumber=1,trigger=\"user A\"",
                statusLine);
    }

    @Test
    public void testNotifyFinalBuildStateFailed() throws IOException {

        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(BuildNotifierConstants.JOB_NAME, "mockjobname");
        parameters.put(BuildNotifierConstants.REPO_OWNER, "mockowner");
        parameters.put(BuildNotifierConstants.REPO_NAME, "mockrepo");
        parameters.put(BuildNotifierConstants.BRANCH_NAME, "mockbranch");
        parameters.put(BuildNotifierConstants.JOB_DURATION, 1010L);
        parameters.put(BuildNotifierConstants.BLOCKED_DURATION, 0L);
        parameters.put(BuildNotifierConstants.BUILD_OBJECT, mockRun);
        instance.notifyFinalBuildStatus(BuildStage.State.CompletedError, parameters);

        verify(mockHttpClient).execute(any());
        assertEquals("job,owner=mockowner,repo=mockrepo,result=CompletedError jobname=\"mockjobname\",branch=\"mockbranch\",blocked=0,jobtime=1010,blockedtime=0,passed=0,buildurl=\"https://jenkins.com/1\",buildnumber=1,trigger=\"user A\"",
                statusLine);
    }

    /**
     * Verifies notifier reports errors that happen outside a stage.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testNonStageError() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(BuildNotifierConstants.JOB_NAME, "mockjobname");
        parameters.put(BuildNotifierConstants.REPO_OWNER, "mockowner");
        parameters.put(BuildNotifierConstants.REPO_NAME, "mockrepo");
        parameters.put(BuildNotifierConstants.BRANCH_NAME, "mockbranch");
        parameters.put(BuildNotifierConstants.STAGE_DURATION, 2020L);
        parameters.put(BuildNotifierConstants.BLOCKED_DURATION, 0L);
        parameters.put(BuildNotifierConstants.BUILD_OBJECT, mockRun);

        BuildStage stageItem = new BuildStage("mockstagename", parameters);
        stageItem.setIsStage(false);
        stageItem.setBuildState(BuildStage.State.CompletedError);
        stageItem.setRun(mockRun);

        instance.notifyBuildStageStatus("mockjobname", stageItem);

        verify(mockHttpClient).execute(any());
        assertEquals("stage,owner=mockowner,repo=mockrepo,stagename=mockstagename,result=CompletedError jobname=\"mockjobname\",branch=\"mockbranch\",stagetime=2020,passed=0,buildurl=\"https://jenkins.com/1\",buildnumber=1,trigger=\"user" +
                        " A\"",
        statusLine);

    }

}
