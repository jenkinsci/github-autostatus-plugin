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
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author jxpearce
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BuildStatusConfig.class})
public class BuildStatusConfigTest {

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getDisplayName method, of class BuildStatusConfig.
     */
    @Test
    public void testGetDisplayName() {
        suppress(method(BuildStatusConfig.class, "load"));
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "Global configuration object for the autostatus plugin";
        String result = instance.getDisplayName();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbUrl method, of class BuildStatusConfig.
     */
    @Test
    public void testInfluxDbUrl() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbUrl(expResult);
        String result = instance.getInfluxDbUrl();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbPassword method, of class BuildStatusConfig.
     */
    @Test
    public void testInfluxDbPassword() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbPassword(expResult);
        String result = instance.getInfluxDbPassword();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbDatabase method, of class BuildStatusConfig.
     */
    @Test
    public void testInfluxDbDatabase() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbDatabase(expResult);
        String result = instance.getInfluxDbDatabase();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbUser method, of class BuildStatusConfig.
     */
    @Test
    public void testInfluxDbUser() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbUser(expResult);
        String result = instance.getInfluxDbUser();
        assertEquals(expResult, result);
    }

    /**
     * Test of get/setInfluxDbRetentionPolicy method, of class
     * BuildStatusConfig.
     */
    @Test
    public void testInfluxDbRetentionPolicy() {
        suppress(method(BuildStatusConfig.class, "load"));
        suppress(method(BuildStatusConfig.class, "save"));
        BuildStatusConfig instance = new BuildStatusConfig();
        String expResult = "mock-value";
        instance.setInfluxDbRetentionPolicy(expResult);
        String result = instance.getInfluxDbRetentionPolicy();
        assertEquals(expResult, result);
    }
}
