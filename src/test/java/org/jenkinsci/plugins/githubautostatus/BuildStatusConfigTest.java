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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import java.io.IOException;
import jenkins.model.GlobalConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GlobalConfiguration.class})
@PowerMockIgnore({"javax.crypto.*"})
public class BuildStatusConfigTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private final String testCredentials = "super-secret-shhh!";
    private final String testInvalidCredentials = "i-dont-exist";
    private final String testCredentialsUser = "papa-jenkins";
    private final String testCredentialsPassword = "1234";

    public BuildStatusConfigTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getDisplayName method, of class BuildStatusConfig.
     */
    @Test
    public void testGetDisplayName() {
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "Global configuration object for the autostatus plugin";
        String result = instance.getDisplayName();
        assertEquals(expResult, result);
    }
    
    @Test
    public void testGetCredentialsId() {
        BuildStatusConfig instance = new BuildStatusConfig();
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
        BuildStatusConfig instance = new BuildStatusConfig();
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
        BuildStatusConfig instance = new BuildStatusConfig();
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
        BuildStatusConfig instance = new BuildStatusConfig();
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
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableGithub(true);
        assertTrue(instance.getEnableGithub());
    }

    /**
     * Verifies round trip get/set of enableGithub
     */
    @Test
    public void testSetEnableGithubFalse() {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableGithub(false);
        assertFalse(instance.getEnableGithub());
    }

    /**
     * Verifies round trip get/set of enableInfluxDb
     */
    @Test
    public void testSetEnableInfluxDbTrue() {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableInfluxDb(true);
        assertTrue(instance.getEnableInfluxDb());
    }

    /**
     * Verifies round trip get/set of enableInfluxDb
     */
    @Test
    public void testSetEnableInfluxDbFalse() {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableInfluxDb(false);
        assertFalse(instance.getEnableInfluxDb());
    }

    /**
     * Verifies doCheckCredentialsId returns OK if empty 
     */
    @Test
    public void testDoCheckCredentialsIdEmpty() {
        BuildStatusConfig instance = new BuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckCredentialsId(null, "").kind);
    }

    /**
     * Verifies doCheckCredentialsId returns OK for credentials in the store 
     * @throws IOException 
     */
    @Test
    public void testDoCheckCredentialsFound() throws IOException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new BuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckCredentialsId(null, testCredentials).kind);
    }

    /**
     * Verifies doCheckCredentialsId returns ERROR for credentials not in the store 
     * @throws IOException 
     */
    @Test
    public void testDoCheckCredentialsNotFound() throws IOException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new BuildStatusConfig();
        assertEquals(Kind.ERROR, instance.doCheckCredentialsId(null, testInvalidCredentials).kind);
    }

    /**
     * Verifies doFillCredentialsIdItems adds the passed in current value
     */
    @Test
    public void testDoFillCredentialsIdItemsAddsCurrent() {
        BuildStatusConfig instance = new BuildStatusConfig();
        
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
    public void testDoFillCredentialsIdItemsAddsFromCredentialsStore() throws IOException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new BuildStatusConfig();
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
        BuildStatusConfig instance = new BuildStatusConfig();
        boolean expectedIgnoreSendingTestCoverageToInflux = false;
        instance.setIgnoreSendingTestResultsToInflux(expectedIgnoreSendingTestCoverageToInflux);
        boolean result = instance.getIgnoreSendingTestCoverageToInflux();
        assertEquals(expectedIgnoreSendingTestCoverageToInflux, result);
    }

    @Test
    public void testIgnoreSendingTestResultsToInfluxDbFalse() {
        BuildStatusConfig instance = new BuildStatusConfig();
        boolean expectedIgnoreSendingTestResultsToInflux = false;
        instance.setIgnoreSendingTestResultsToInflux(expectedIgnoreSendingTestResultsToInflux);
        boolean result = instance.getIgnoreSendingTestResultsToInflux();
        assertEquals(expectedIgnoreSendingTestResultsToInflux, result);
    }

    @Test
    public void testIgnoreSendingTestCoverageToInfluxDbTrue() {
        BuildStatusConfig instance = new BuildStatusConfig();
        boolean expectedIgnoreSendingTestCoverageToInflux = true;
        instance.setIgnoreSendingTestCoverageToInflux(expectedIgnoreSendingTestCoverageToInflux);
        boolean result = instance.getIgnoreSendingTestCoverageToInflux();
        assertEquals(expectedIgnoreSendingTestCoverageToInflux, result);
    }

    @Test
    public void testIgnoreSendingTestResultsToInfluxDbTrue() {
        BuildStatusConfig instance = new BuildStatusConfig();
        boolean expectedIgnoreSendingTestResultsToInflux = true;
        instance.setIgnoreSendingTestCoverageToInflux(expectedIgnoreSendingTestResultsToInflux);
        boolean result = instance.getIgnoreSendingTestResultsToInflux();
        assertEquals(expectedIgnoreSendingTestResultsToInflux, result);
    }

    @Test
    public void testGetEnableStatsd() throws IOException {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableStatsd(false);
        assertFalse(instance.getEnableStatsd());
    }

    @Test
    public void testStatsdUrl() {
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdHost(expResult);
        String result = instance.getStatsdHost();
        assertEquals(expResult, result);
    }

    @Test
    public void testStatsdPort() {
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdPort(expResult);
        String result = instance.getStatsdPort();
        assertEquals(expResult, result);
    }

    @Test
    public void testStatsdBucket() {
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setStatsdBucket(expResult);
        String result = instance.getStatsdBucket();
        assertEquals(expResult, result);
    }

    @Test
    public void testStatsdMaxSize() {
        BuildStatusConfig instance = new BuildStatusConfig();
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
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableHttp(true);
        assertTrue(instance.getEnableHttp());
    }

    /**
     * Verifies round trip get/set of enableHttp
     */
    @Test
    public void testSetEnableHttpFalse() {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableHttp(false);
        assertFalse(instance.getEnableHttp());
    }

    /**
     * Verifies round trip get/set of HttpVerifySSL
     */
    @Test
    public void testSetHttpVerifySSLTrue() {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setHttpVerifySSL(true);
        assertTrue(instance.getHttpVerifySSL());
    }

    /**
     * Verifies round trip get/set of HttpVerifySSL
     */
    @Test
    public void testSetHttpVerifySSLFalse() {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setHttpVerifySSL(false);
        assertFalse(instance.getEnableHttp());
    }

    @Test
    public void testHttpGetCredentialsId() {
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setHttpCredentialsId(expResult);
        String result = instance.getHttpCredentialsId();
        assertEquals(expResult, result);
    }

    /**
     * Verifies doCheckHttpCredentialsId returns OK if empty
     */
    @Test
    public void testDoCheckHttpCredentialsIdEmpty() {
        BuildStatusConfig instance = new BuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckHttpCredentialsId(null, "").kind);
    }

    /**
     * Verifies doCheckHttpCredentialsId returns OK for credentials in the store
     * @throws IOException
     */
    @Test
    public void testDoCheckHttpCredentialsFound() throws IOException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new BuildStatusConfig();
        assertEquals(Kind.OK, instance.doCheckHttpCredentialsId(null, testCredentials).kind);
    }

    /**
     * Verifies doCheckHttpCredentialsId returns ERROR for credentials not in the store
     * @throws IOException
     */
    @Test
    public void testDoCheckHttpCredentialsNotFound() throws IOException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new BuildStatusConfig();
        assertEquals(Kind.ERROR, instance.doCheckHttpCredentialsId(null, testInvalidCredentials).kind);
    }

    /**
     * Verifies doFillHttpCredentialsIdItems adds the passed in current value
     */
    @Test
    public void testDoFillHttpCredentialsIdItemsAddsCurrent() {
        BuildStatusConfig instance = new BuildStatusConfig();

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
    public void testDoFillHttpCredentialsIdItemsAddsFromCredentialsStore() throws IOException {
        StandardUsernameCredentials user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, testCredentials, "Description", testCredentialsUser, testCredentialsPassword);
        CredentialsProvider.lookupStores(j.getInstance()).iterator().next().addCredentials(Domain.global(), user);

        BuildStatusConfig instance = new BuildStatusConfig();
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
     * Test of get/set httpEndpoint  method, of class BuildStatusConfig.
     */
    @Test
    public void testHttpEndpoint () {
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "https://mock.com";
        instance.setHttpEndpoint(expResult);
        String result = instance.getHttpEndpoint();
        assertEquals(expResult, result);
    }

    @Test
    public void testDoCheckHttpEndpointEmpty(){
        BuildStatusConfig instance = new BuildStatusConfig();
        FormValidation result = instance.doCheckHttpEndpoint(null, "");
        assertEquals(Kind.ERROR, result.kind);
    }

    @Test
    public void testDoCheckHttpEndpointValid(){
        BuildStatusConfig instance = new BuildStatusConfig();
        FormValidation result = instance.doCheckHttpEndpoint(null, "https://mock.com:8443/api?token=1q2w3e");
        assertEquals(Kind.OK, result.kind);
    }

    @Test
    public void testDoCheckHttpEndpointInvalid(){
        BuildStatusConfig instance = new BuildStatusConfig();
        FormValidation result = instance.doCheckHttpEndpoint(null, "mock.com/api?token=1q2w3e");
        assertEquals(Kind.ERROR, result.kind);
    }
}
