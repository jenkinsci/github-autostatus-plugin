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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author jxpearce
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BuildStatusConfig.class})
@PowerMockIgnore("javax.*")
public class InfluxDbNotifierConfigTest {

    private final String repositoryOwner = "mock-pwner";
    private final String repository = "mock-repo";
    private final String branch = "mock-branch";
    private final String influxUrl = "mock-url";
    private final String influxDatabase = "mock-usdatabase";
    private final String influxDbUser = "mock-user";
    private final String influxDbPassword = "mock-password";
    private final String influxDbRetention = "mock-retention-policy";

    public InfluxDbNotifierConfigTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(BuildStatusConfig.class);
        BuildStatusConfig config = mock(BuildStatusConfig.class);
        when(BuildStatusConfig.get()).thenReturn(config);

        when(config.getEnableInfluxDb()).thenReturn(true);
        when(config.getInfluxDbUrl()).thenReturn(influxUrl);
        when(config.getInfluxDbDatabase()).thenReturn(influxDatabase);
        when(config.getInfluxDbUser()).thenReturn(influxDbUser);
        when(config.getInfluxDbPassword()).thenReturn(influxDbPassword);
        when(config.getInfluxDbRetentionPolicy()).thenReturn(influxDbRetention);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetRepoOwner() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig(repositoryOwner, "", "");
        String result = instance.getRepoOwner();
        assertEquals(repositoryOwner, result);
    }

    @Test
    public void testGetRepoName() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", repository, "");
        String result = instance.getRepoName();
        assertEquals(repository, result);
    }

    @Test
    public void testGetBranchName() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        String result = instance.getBranchName();
        assertEquals(branch, result);
    }

    @Test
    public void testGetInfluxDbUrlString() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertEquals(influxUrl, instance.getInfluxDbUrlString());
    }

    @Test
    public void testGetInfluxDbDatabase() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertEquals(influxDatabase, instance.getInfluxDbDatabase());
    }

    @Test
    public void testGetInfluxDbUser() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertEquals(influxDbUser, instance.getInfluxDbUser());
    }

    @Test
    public void testGetInfluxDbPassword() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertEquals(influxDbPassword, instance.getInfluxDbPassword());
    }

    @Test
    public void testGetInfluxDbRetentionPolicy() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertEquals(influxDbRetention, instance.getInfluxDbRetentionPolicy());
    }

    @Test
    public void testGetHttpClient() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertNotNull(instance.getHttpClient());
    }

    @Test
    public void testinfluxDbIsReachableFalse() {
        InfluxDbNotifierConfig instance
                = InfluxDbNotifierConfig.fromGlobalConfig("", "", branch);
        assertFalse(instance.influxDbIsReachable());

    }
}
