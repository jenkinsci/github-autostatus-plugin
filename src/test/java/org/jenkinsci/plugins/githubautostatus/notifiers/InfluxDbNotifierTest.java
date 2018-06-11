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
import java.net.MalformedURLException;
import java.util.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.githubautostatus.InfluxDbNotifierConfig;
import org.junit.After;
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
 * @author jxpearce
 */
public class InfluxDbNotifierTest {

    private InfluxDbNotifierConfig config;
    private final String influxDbCredentialsId = "mock-credentials";
    private final String influxDbUser = "mock-user";
    private final String influxDbPassword = "mock-password";
    private String statusLine;
    private CloseableHttpClient mockHttpClient;
    private StatusLine mockStatusLine;

    public InfluxDbNotifierTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws MalformedURLException, IOException {
        statusLine = null;
        config = mock(InfluxDbNotifierConfig.class);
        when(config.influxDbIsReachable()).thenReturn(true);
        when(config.getInfluxDbUrlString()).thenReturn("http://fake");
        when(config.getInfluxDbDatabase()).thenReturn("mockdb");
        when(config.getRepoOwner()).thenReturn("mockowner");
        when(config.getRepoName()).thenReturn("mockrepo");
        when(config.getBranchName()).thenReturn("mockbranch");

        mockHttpClient = mock(CloseableHttpClient.class);
        when(config.getHttpClient()).thenReturn(mockHttpClient);

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

    }

    @After
    public void tearDown() {
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
    public void testIsEnabled() throws Exception {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertTrue(instance.isEnabled());
    }

    @Test
    public void testIsDisabledUrl() throws Exception {
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
        UsernamePasswordCredentials credentials = 
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, 
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
    public void testUrlRetention() throws Exception {
        when(config.getInfluxDbRetentionPolicy()).thenReturn("mockretention");
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        assertEquals("http://fake/write?db=mockdb&rp=mockretention", instance.influxDbUrlString);
    }

    @Test
    public void testNotifyBuildState() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        instance.notifyBuildState("mockjobname", "mocknodename", BuildState.CompletedSuccess);

        verify(mockHttpClient).execute(any());
        assertEquals(statusLine,
                "stage,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,stagename=mocknodename,result=CompletedSuccess stagetime=0,passed=1");
    }

    @Test
    public void testNotifyBuildStateHttpError() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        when(mockStatusLine.getStatusCode()).thenReturn(400);

        instance.notifyBuildState("mockjobname", "mocknodename", BuildState.CompletedSuccess);

        verify(mockHttpClient).execute(any());
        assertEquals(statusLine,
                "stage,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,stagename=mocknodename,result=CompletedSuccess stagetime=0,passed=1");
    }

    @Test
    public void testNotifyBuildStateIoException() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);
        when(mockStatusLine.getStatusCode()).thenAnswer((InvocationOnMock invocation) -> {
            throw new IOException();
        });

        instance.notifyBuildState("mockjobname", "mocknodename", BuildState.CompletedSuccess);

        verify(mockHttpClient).execute(any());
        assertEquals(statusLine,
                "stage,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,stagename=mocknodename,result=CompletedSuccess stagetime=0,passed=1");
    }

    @Test
    public void testNotifyBuildStatePending() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        instance.notifyBuildState("mockjobname", "mocknodename", BuildState.Pending);

        verify(mockHttpClient, never()).execute(any());
        assertNull(statusLine);
    }

    @Test
    public void testNotifyFinalBuildStateSuccess() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        instance.notifyFinalBuildStatus("mockjobname", BuildState.CompletedSuccess, 100);

        verify(mockHttpClient).execute(any());
        assertEquals(statusLine,
                "job,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,result=CompletedSuccess jobtime=100,passed=1");
    }

    @Test
    public void testNotifyFinalBuildStateFailed() throws IOException {

        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        instance.notifyFinalBuildStatus("mockjobname", BuildState.CompletedError, 1010);

        verify(mockHttpClient).execute(any());
        assertEquals(statusLine,
                "job,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,result=CompletedError jobtime=1010,passed=0");
    }

    @Test
    public void testNotifyNonStageError() throws IOException {
        InfluxDbNotifier instance = new InfluxDbNotifier(config);

        instance.sendNonStageError("mockjobname", "mockstage");

        verify(mockHttpClient).execute(any());
        assertEquals(statusLine,
                "stage,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,stagename=mockstage,result=CompletedError stagetime=0,passed=0");
    }
}
