
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
 * @author shane.gearon@hootsuite.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BuildStatusConfig.class})
@PowerMockIgnore("javax.*")
public class StatsdNotifierConfigTest {

    private BuildStatusConfig config;
    private final String externalizedID = "mock-id/mock-path#test";
    private final String statsdURL = "statsd.url";
    private final String statsdPort = "9999";
    private final String statsdBucket = "metrics.jenkins.";
    private final String statsdMaxSize = "1000";

    public StatsdNotifierConfigTest() {
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
        config = mock(BuildStatusConfig.class);
        when(BuildStatusConfig.get()).thenReturn(config);

        when(config.getEnableStatsd()).thenReturn(true);
        when(config.getStatsdHost()).thenReturn(statsdURL);
        when(config.getStatsdPort()).thenReturn(statsdPort);
        when(config.getStatsdBucket()).thenReturn(statsdBucket);
        when(config.getStatsdMaxSize()).thenReturn(statsdMaxSize);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetExternalizedId() {
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        String result = instance.getExternalizedID();
        assertEquals(externalizedID, result);
    }

    @Test
    public void testGetStatsdHost() {
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(statsdURL, instance.getStatsdHost());
    }

    @Test
    public void testGetStatsdDisabled() {
        when(config.getEnableStatsd()).thenReturn(false);
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(null, instance);
    }

    @Test
    public void testGetStatsdHostEmpty() {
        when(config.getStatsdHost()).thenReturn("");
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(null, instance);
    }

    @Test
    public void testGetStatsdPort() {
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(9999, instance.getStatsdPort());
    }

    @Test
    public void testGetStatsdPortEmpty() {
        when(config.getStatsdPort()).thenReturn("");
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(8125, instance.getStatsdPort());
    }

    @Test
    public void testGetStatsdPortNull() {
        when(config.getStatsdPort()).thenReturn(null);
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(8125, instance.getStatsdPort());
    }

    @Test
    public void testGetStatsdPortNotANumber() {
        when(config.getStatsdPort()).thenReturn("notANumber");
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(8125, instance.getStatsdPort());
    }


    @Test
    public void testGetStatsdBucket() {
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(statsdBucket, instance.getStatsdBucket());
    }

    @Test
    public void testGetStatsdMaxSize() {
        StatsdNotifierConfig instance
                = StatsdNotifierConfig.fromGlobalConfig(externalizedID);
        assertEquals(statsdMaxSize, instance.getStatsdMaxSize());
    }
}
