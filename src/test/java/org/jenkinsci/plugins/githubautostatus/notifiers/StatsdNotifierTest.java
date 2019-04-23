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

import static org.junit.Assert.*;

import java.io.IOException;

import com.timgroup.statsd.StatsDClient;

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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.StatsdWrapper;
import org.jenkinsci.plugins.githubautostatus.notifiers.StatsdNotifier;

/**
 *
 * @author shane.gearon@hootsuite.com
 */
public class StatsdNotifierTest {

    private StatsdNotifierConfig config;
    private StatsdWrapper wrapper;
    private StatsDClient client;

    public StatsdNotifierTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        config = mock(StatsdNotifierConfig.class);
        when(config.getBranchName()).thenReturn("a////<>\\|;:%/!@#$%^&*()+=//...////....b");
        when(config.getRepoOwner()).thenReturn("folder0 / folder1 /     folder.2/ folder  3");
        when(config.getRepoName()).thenReturn("this .   is ... the ... reponame");

        mockClient = mock(StatsdWrapper.class);
        notifier = new StatsdNotifier(mockClient);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSanitizeAll() throws IOException {
        String out = notifier.sanitizeAll(config.getBranchName());
        assertEquals("a.b", out);
        out = notifier.sanitizeAll(config.getRepoName());
        assertEquals(out, "this_is_the_reponame");
        out = notifier.sanitizeAll(config.getRepoOwner());
        // path sanitization should come first, leading and following whitespaces should
        // be turned into a single underscore.
        assertEquals("folder0_._folder1_._folder2._folder_3", out);
    }
    public void setUp() throws IOException {
        config = mock(StatsdNotifierConfig.class);
        when(config.getStatsdBucket()).thenReturn("bucket");
        when(config.getStatsdHost()).thenReturn("hostname.test");
        mock(StatsdWrapper.class);
    }

    /**
     * Test empty endpoint disables config.
     */
    @Test
    public void testDisabledEmptyConfig() {
        when(config.getStatsdHost()).thenReturn("");
        StatsdNotifier instance = new StatsdNotifier(config);
        assertFalse(instance.isEnabled());
    }

    /**
     * Test valid endpoint enables config.
     */
    @Test
    public void testIsEnabled() throws Exception {
        wrapper = mock(StatsdWrapper.class);
        when(new StatsdWrapper("bucket", "hostname.test", 8125)).thenReturn(wrapper);
        StatsdNotifier instance = new StatsdNotifier(config);
        assertTrue(instance.isEnabled());
    }

    /**
     * Test empty port is default port 8125
     */
    @Test
    public void testEmptyPort() throws Exception {
        when(config.getStatsdPort()).thenReturn(null);
        StatsdNotifier instance = new StatsdNotifier(config);
        assertArrayEquals(instance.port, 8125);
    }


}
