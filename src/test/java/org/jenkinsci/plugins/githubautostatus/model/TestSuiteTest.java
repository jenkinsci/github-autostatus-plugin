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

import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class TestSuiteTest {

    private final String mockName = "mock-name";

    public TestSuiteTest() {
    }

    @Test
    public void testGetNameInitial() {
        TestSuite instance = new TestSuite();
        assertNull("", instance.getName());
    }

    @Test
    public void testGetSetName() {
        TestSuite instance = new TestSuite();
        instance.setName(mockName);
        assertEquals(mockName, instance.getName());
    }

    @Test
    public void testGetSetDuration() {
        float duration = 435;

        TestSuite instance = new TestSuite();
        instance.setDuration(duration);
        assertEquals(duration, instance.getDuration(), 0);
    }

    @Test
    public void testGetTestCasesInitial() {
        TestSuite instance = new TestSuite();
        ArrayList<TestCase> result = instance.getTestCases();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAddTestCases() {
        TestSuite instance = new TestSuite();
        instance.addTestCases(new TestCase());
        ArrayList<TestCase> result = instance.getTestCases();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testAddTestCasesCounts() {
        TestCase mockTestCase = mock(TestCase.class);
        when(mockTestCase.getPassedCount()).thenReturn(242);
        when(mockTestCase.getFailedCount()).thenReturn(12);
        when(mockTestCase.getSkippedCount()).thenReturn(14);

        TestSuite instance = new TestSuite();
        instance.addTestCases(mockTestCase);
        ArrayList<TestCase> result = instance.getTestCases();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(242, result.get(0).getPassedCount());
        assertEquals(12, result.get(0).getFailedCount());
        assertEquals(14, result.get(0).getSkippedCount());
    }

    @Test
    public void testGetSetPassedTestCaseCount() {
        int count = 2112;

        TestSuite instance = new TestSuite();
        instance.setPassedTestCaseCount(count);
        assertEquals(count, instance.getPassedTestCaseCount());
    }

    @Test
    public void testGetSetSkippedTestCaseCount() {
        int count = 2008;
        
        TestSuite instance = new TestSuite();
        instance.setSkippedTestCaseCount(count);
        assertEquals(count, instance.getSkippedTestCaseCount());
    }

    @Test
    public void testGetSetFailedTestCaseCount() {
        int count = 1973;
        
        TestSuite instance = new TestSuite();
        instance.setFailedTestCaseCount(count);
        assertEquals(count, instance.getFailedTestCaseCount());
    }
}
