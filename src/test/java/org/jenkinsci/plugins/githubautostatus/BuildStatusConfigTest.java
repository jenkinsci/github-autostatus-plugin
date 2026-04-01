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
package org.jenkinsci.plugins.githubautostatus;

import static org.junit.jupiter.api.Assertions.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@WithJenkins
public class BuildStatusConfigTest {

    private final String testCredentials = "super-secret-shhh!";
    private final String testInvalidCredentials = "i-dont-exist";
    private final String testCredentialsUser = "papa-jenkins";
    private final String testCredentialsPassword = "1234";

    /**
     * Test subclass to avoid persistence calls during unit tests.
     */
    static class TestBuildStatusConfig extends BuildStatusConfig {
        @Override
        public synchronized void load() {
            // no-op for tests
        }

        @Override
        public synchronized void save() {
            // no-op for tests
        }
    }

    /**
     * Test of getDisplayName method, of class BuildStatusConfig.
     */
    @Test
    public void testGetDisplayName() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "Global configuration object for the autostatus plugin";
        String result = instance.getDisplayName();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetCredentialsId() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setCredentialsId(expResult);
        String result = instance.getCredentialsId();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbUrl method, of class BuildStatusConfig.
     */
    @Test
    public void testInfluxDbUrl() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbUrl(expResult);
        String result = instance.getInfluxDbUrl();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbDatabase method, of class BuildStatusConfig.
     */
    @Test
    public void testInfluxDbDatabase() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbDatabase(expResult);
        String result = instance.getInfluxDbDatabase();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbRetentionPolicy method, of class
     * BuildStatusConfig.
     */
    @Test
    public void testInfluxDbRetentionPolicy() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbRetentionPolicy(expResult);
        String result = instance.getInfluxDbRetentionPolicy();
        assertEquals(expResult, result);
    }

    /**
     * Verifies round trip get/set of enableGithub
     */
    @Test
    public void testSetEnableGithubTrue() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableGithub(true);
        assertTrue(instance.getEnableGithub());
    }

    /**
     * Verifies round trip get/set of enableGithub
     */
    @Test
    public void testSetEnableGithubFalse() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableGithub(false);
        assertFalse(instance.getEnableGithub());
    }

    /**
     * Verifies round trip get/set of enableInfluxDb
     */
    @Test
    public void testSetEnableInfluxDbTrue() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableInfluxDb(true);
        assertTrue(instance.getEnableInfluxDb());
    }

    /**
     * Verifies round trip get/set of enableInfluxDb
     */
    @Test
    public void testSetEnableInfluxDbFalse() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableInfluxDb(false);
        assertFalse(instance.getEnableInfluxDb());
    }

    /**
     * Verifies doCheckCredentialsId returns OK if empty
     */
    @Test
    public void testDoCheckCredentialsIdEmpty(JenkinsRule j) {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckCredentialsId(null, "").kind);
    }

    /**
     * Verifies doCheckCredentialsId returns OK for credentials in the store
     * @throws IOException
     */
    @Test
    public void testDoCheckCredentialsFound(JenkinsRule j) throws IOException, FormException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new TestBuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckCredentialsId(null, testCredentials).kind);
    }

    /**
     * Verifies doCheckCredentialsId returns ERROR for credentials not in the store
     * @throws IOException
     */
    @Test
    public void testDoCheckCredentialsNotFound(JenkinsRule j) throws IOException, FormException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new TestBuildStatusConfig();
        assertEquals(Kind.ERROR, instance.doCheckCredentialsId(null, testInvalidCredentials).kind);
    }

    /**
     * Verifies doFillCredentialsIdItems adds the passed in current value
     */
    @Test
    public void testDoFillCredentialsIdItemsAddsCurrent(JenkinsRule j) {
        BuildStatusConfig instance = new TestBuildStatusConfig();

        final String currentValue = "mock-id";
        ListBoxModel model = instance.doFillCredentialsIdItems(currentValue);

        assertEquals(2, model.size());
        ListBoxModel.Option item1 = model.get(0);
        assertEquals("", item1.value);
        assertEquals("- none -", item1.name);

        ListBoxModel.Option item2 = model.get(1);
        assertEquals(currentValue, item2.value);
    }

    /**
     * Verifies doFillCredentialsIdItems adds values from the credentials store
     * @throws IOException
     */
    @Test
    public void testDoFillCredentialsIdItemsAddsFromCredentialsStore(JenkinsRule j) throws IOException, FormException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setCredentialsId(testCredentials);

        ListBoxModel model = instance.doFillCredentialsIdItems(testCredentials);

        assertEquals(2, model.size());
        ListBoxModel.Option item1 = model.get(0);
        assertEquals("", item1.value);
        assertEquals("- none -", item1.name);

        ListBoxModel.Option item2 = model.get(1);
        assertEquals(testCredentials, item2.value);
    }

    @Test
    public void testIgnoreSendingTestCoverageToInfluxDbFalse() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        boolean expectedIgnoreSendingTestCoverageToInflux = false;
        instance.setIgnoreSendingTestCoverageToInflux(expectedIgnoreSendingTestCoverageToInflux);
        boolean result = instance.getIgnoreSendingTestCoverageToInflux();
        assertEquals(expectedIgnoreSendingTestCoverageToInflux, result);
    }

    @Test
    public void testIgnoreSendingTestResultsToInfluxDbFalse() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        boolean expectedIgnoreSendingTestResultsToInflux = false;
        instance.setIgnoreSendingTestResultsToInflux(expectedIgnoreSendingTestResultsToInflux);
        boolean result = instance.getIgnoreSendingTestResultsToInflux();
        assertEquals(expectedIgnoreSendingTestResultsToInflux, result);
    }

    @Test
    public void testIgnoreSendingTestCoverageToInfluxDbTrue() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        boolean expectedIgnoreSendingTestCoverageToInflux = true;
        instance.setIgnoreSendingTestCoverageToInflux(expectedIgnoreSendingTestCoverageToInflux);
        boolean result = instance.getIgnoreSendingTestCoverageToInflux();
        assertEquals(expectedIgnoreSendingTestCoverageToInflux, result);
    }

    @Test
    public void testIgnoreSendingTestResultsToInfluxDbTrue() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        boolean expectedIgnoreSendingTestResultsToInflux = true;
        instance.setIgnoreSendingTestResultsToInflux(expectedIgnoreSendingTestResultsToInflux);
        boolean result = instance.getIgnoreSendingTestResultsToInflux();
        assertEquals(expectedIgnoreSendingTestResultsToInflux, result);
    }

    @Test
    public void testGetEnableStatsd() throws IOException {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableStatsd(false);
        assertFalse(instance.getEnableStatsd());
    }

    @Test
    public void testStatsdUrl() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdHost(expResult);
        String result = instance.getStatsdHost();
        assertEquals(expResult, result);
    }

    @Test
    public void testStatsdPort() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdPort(expResult);
        String result = instance.getStatsdPort();
        assertEquals(expResult, result);
    }

    @Test
    public void testStatsdBucket() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdBucket(expResult);
        String result = instance.getStatsdBucket();
        assertEquals(expResult, result);
    }

    @Test
    public void testStatsdMaxSize() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdMaxSize(expResult);
        String result = instance.getStatsdMaxSize();
        assertEquals(expResult, result);
    }

    /**
     * Verifies round trip get/set of enableHttp
     */
    @Test
    public void testSetEnableHttpTrue() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableHttp(true);
        assertTrue(instance.getEnableHttp());
    }

    /**
     * Verifies round trip get/set of enableHttp
     */
    @Test
    public void testSetEnableHttpFalse() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setEnableHttp(false);
        assertFalse(instance.getEnableHttp());
    }

    /**
     * Verifies round trip get/set of HttpVerifySSL
     */
    @Test
    public void testSetHttpVerifySSLTrue() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setHttpVerifySSL(true);
        assertTrue(instance.getHttpVerifySSL());
    }

    /**
     * Verifies round trip get/set of HttpVerifySSL
     */
    @Test
    public void testSetHttpVerifySSLFalse() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setHttpVerifySSL(false);
        assertFalse(instance.getEnableHttp());
    }

    @Test
    public void testHttpGetCredentialsId() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "mock-value";
        instance.setHttpCredentialsId(expResult);
        String result = instance.getHttpCredentialsId();
        assertEquals(expResult, result);
    }

    /**
     * Verifies doCheckHttpCredentialsId returns OK if empty
     */
    @Test
    public void testDoCheckHttpCredentialsIdEmpty(JenkinsRule j) {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckHttpCredentialsId(null, "").kind);
    }

    /**
     * Verifies doCheckHttpCredentialsId returns OK for credentials in the store
     * @throws IOException
     */
    @Test
    public void testDoCheckHttpCredentialsFound(JenkinsRule j) throws IOException, FormException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new TestBuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckHttpCredentialsId(null, testCredentials).kind);
    }

    /**
     * Verifies doCheckHttpCredentialsId returns ERROR for credentials not in the store
     * @throws IOException
     */
    @Test
    public void testDoCheckHttpCredentialsNotFound(JenkinsRule j) throws IOException, FormException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new TestBuildStatusConfig();
        assertEquals(Kind.ERROR, instance.doCheckHttpCredentialsId(null, testInvalidCredentials).kind);
    }

    /**
     * Verifies doFillHttpCredentialsIdItems adds the passed in current value
     */
    @Test
    public void testDoFillHttpCredentialsIdItemsAddsCurrent(JenkinsRule j) {
        BuildStatusConfig instance = new TestBuildStatusConfig();

        final String currentValue = "mock-id";
        ListBoxModel model = instance.doFillHttpCredentialsIdItems(currentValue);

        assertEquals(2, model.size());
        ListBoxModel.Option item1 = model.get(0);
        assertEquals("", item1.value);
        assertEquals("- none -", item1.name);

        ListBoxModel.Option item2 = model.get(1);
        assertEquals(currentValue, item2.value);
    }

    /**
     * Verifies doFillCredentialsIdItems adds values from the credentials store
     * @throws IOException
     */
    @Test
    public void testDoFillHttpCredentialsIdItemsAddsFromCredentialsStore(JenkinsRule j)
            throws IOException, FormException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new TestBuildStatusConfig();
        instance.setCredentialsId(testCredentials);

        ListBoxModel model = instance.doFillHttpCredentialsIdItems(testCredentials);

        assertEquals(2, model.size());
        ListBoxModel.Option item1 = model.get(0);
        assertEquals("", item1.value);
        assertEquals("- none -", item1.name);

        ListBoxModel.Option item2 = model.get(1);
        assertEquals(testCredentials, item2.value);
    }

    /**
     * Test of get/set httpEndpoint method, of class BuildStatusConfig.
     */
    @Test
    public void testHttpEndpoint() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        String expResult = "https://mock.com";
        instance.setHttpEndpoint(expResult);
        String result = instance.getHttpEndpoint();
        assertEquals(expResult, result);
    }

    @Test
    public void testDoCheckHttpEndpointEmpty() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        FormValidation result = instance.doCheckHttpEndpoint(null, "");
        assertEquals(Kind.ERROR, result.kind);
    }

    @Test
    public void testDoCheckHttpEndpointValid() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        FormValidation result = instance.doCheckHttpEndpoint(null, "https://mock.com:8443/api?token=1q2w3e");
        assertEquals(Kind.OK, result.kind);
    }

    @Test
    public void testDoCheckHttpEndpointInvalid() {
        BuildStatusConfig instance = new TestBuildStatusConfig();
        FormValidation result = instance.doCheckHttpEndpoint(null, "mock.com/api?token=1q2w3e");
        assertEquals(Kind.ERROR, result.kind);
    }
}
