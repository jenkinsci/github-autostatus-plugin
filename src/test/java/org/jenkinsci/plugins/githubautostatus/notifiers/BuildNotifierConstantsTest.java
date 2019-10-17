/*
 * The MIT License
 *
 * Copyright 2019 jxpearce.
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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildNotifierConstantsTest {
    
    public BuildNotifierConstantsTest() {
    }
    
    @Test
    public void testConstants() {
        new BuildNotifierConstants();
        assertNotNull(BuildNotifierConstants.BLOCKED_DURATION);
        assertNotNull(BuildNotifierConstants.BRANCH_NAME);
        assertNotNull(BuildNotifierConstants.BUILD_OBJECT);
        assertNotNull(BuildNotifierConstants.COVERAGE_INFO);
        assertNotNull(BuildNotifierConstants.JOB_DURATION);
        assertNotNull(BuildNotifierConstants.JOB_NAME);
        assertNotNull(BuildNotifierConstants.REPO_NAME);
        assertNotNull(BuildNotifierConstants.REPO_OWNER);
        assertNotNull(BuildNotifierConstants.STAGE_DURATION);
        assertNotNull(BuildNotifierConstants.TEST_CASE_INFO);
        assertEquals(BuildNotifierConstants.DEFAULT_STRING, "none");
        assertEquals(BuildNotifierConstants.DEFAULT_LONG, 0L);
    }

    @Test
    public void testGetLong(){
        Map<String, Object> map = new HashMap<>();
        long expectedResult = 1234L;
        map.put("key", expectedResult);
        assertEquals(expectedResult, BuildNotifierConstants.getLong(map, "key"));
    }

    @Test
    public void testGetLongWithDefaultValue(){
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        assertEquals(BuildNotifierConstants.DEFAULT_LONG, BuildNotifierConstants.getLong(map, "not-exist"));
    }
}
