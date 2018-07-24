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
 * @author jxpearce
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
    public void setUp() throws IOException {
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
     * @throws IOException 
     */
    @Test
    public void testSetEnableGithubTrue() throws IOException {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableGithub(true);
        assertTrue(instance.getEnableGithub());
    }

    /**
     * Verifies round trip get/set of enableGithub
     * @throws IOException 
     */
    @Test
    public void testSetEnableGithubFalse() throws IOException {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableGithub(false);
        assertFalse(instance.getEnableGithub());
    }

    /**
     * Verifies round trip get/set of enableInfluxDb
     * @throws IOException 
     */
    @Test
    public void testSetEnableInfluxDbTrue() throws IOException {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableInfluxDb(true);
        assertTrue(instance.getEnableInfluxDb());
    }

    /**
     * Verifies round trip get/set of enableInfluxDb
     * @throws IOException 
     */
    @Test
    public void testSetEnableInfluxDbFalse() throws IOException {
        BuildStatusConfig instance = new BuildStatusConfig();
        instance.setEnableInfluxDb(false);
        assertFalse(instance.getEnableInfluxDb());
    }

    /**
     * Verifies doCheckCredentialsId returns OK if empty 
     * @throws IOException 
     */
    @Test
    public void testDoCheckCredentialsIdEmpty() throws IOException {
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
     * @throws IOException 
     */
    @Test
    public void testDoFillCredentialsIdItemsAddsCurrent() throws IOException {
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
}
