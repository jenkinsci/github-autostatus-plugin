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
package org.jenkinsci.plugins.githubautostatus.model;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.tasks.junit.CaseResult;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class TestCaseTest {

    public TestCaseTest() {
    }

    @Test
    public void testInitialPassedTrue() {
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isPassed()).thenReturn(true);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertTrue(result.isPassed());
        assertEquals(result.getResult(), TestCase.TestCaseResult.Passed);
    }

    @Test
    public void testInitialName() {
        String testName = "mock-name";
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.getFullName()).thenReturn(testName);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertEquals(testName, result.getName());
        assertNull(result.getResult());
    }

    @Test
    public void testInitialPassedFalse() {
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isPassed()).thenReturn(false);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertFalse(result.isPassed());
        assertNull(result.getResult());
    }

    @Test
    public void testInitialSkippedTrue() {
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isSkipped()).thenReturn(true);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertTrue(result.isSkipped());
        assertEquals(result.getResult(), TestCase.TestCaseResult.Skipped);
    }

    @Test
    public void testInitialSkippedFalse() {
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isSkipped()).thenReturn(false);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertFalse(result.isSkipped());
        assertNull(result.getResult());
    }

    @Test
    public void testInitialFailedTrue() {
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isFailed()).thenReturn(true);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertTrue(result.isFailed());
        assertEquals(result.getResult(), TestCase.TestCaseResult.Failed);
    }

    @Test
    public void testInitialFailedFalse() {
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isFailed()).thenReturn(false);
        TestCase result = TestCase.fromCaseResult(caseResult);
        assertFalse(result.isFailed());
        assertNull(result.getResult());
    }

}
