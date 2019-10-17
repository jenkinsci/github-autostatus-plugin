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

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SuiteResult.class,TestResult.class })
public class TestResultsTest {

    public TestResultsTest() {
    }

    @Test
    public void testGetSetPassedTestCaseCount() {
        int count = 2345;
        TestResults instance = new TestResults();
        instance.setPassedTestCaseCount(count);
        assertEquals(count, instance.getPassedTestCaseCount());
    }

    @Test
    public void testGetSetSkippedTestCaseCount() {
        int count = 3456;
        TestResults testSuite = new TestResults();
        testSuite.setSkippedTestCaseCount(count);
        assertEquals(count, testSuite.getSkippedTestCaseCount());
    }

    @Test
    public void testGetSetFailedTestCaseCount() {
        int count = 6789;
        TestResults testSuite = new TestResults();
        testSuite.setFailedTestCaseCount(count);
        assertEquals(count, testSuite.getFailedTestCaseCount());
    }

    @Test
    public void testGetSetTestSuites() {
        TestResults testSuite = new TestResults();
        ArrayList<TestSuite> testSuites = new ArrayList<>();
        testSuite.setTestSuites(testSuites);
        assertEquals(testSuites, testSuite.getTestSuites());
    }

    @Test
    public void testfromJUnitTestResultsNull() {
        TestResults instance = TestResults.fromJUnitTestResults(null);
        
        assertNull(instance);
    }

    @Test
    public void testfromJUnitTestResultsEmpty() {
        TestResultAction testResultAction = mock(TestResultAction.class);
        when(testResultAction.getResult()).thenReturn(new TestResult());

        TestResults instance = TestResults.fromJUnitTestResults(testResultAction);
        
        assertNotNull(instance);
        assertEquals(0, instance.getPassedTestCaseCount());
        assertEquals(0, instance.getSkippedTestCaseCount());
        assertEquals(0, instance.getFailedTestCaseCount());
    }

    @Test
    public void testfromJUnitTestResultsNotEmpty() {
        TestResultAction testResultAction = mock(TestResultAction.class);
        TestResult testResult = PowerMockito.mock(TestResult.class);
        SuiteResult suiteResult = PowerMockito.mock(SuiteResult.class);
        ArrayList<SuiteResult> suiteResults = new ArrayList<>();
        suiteResults.add(suiteResult);
        
        ArrayList<CaseResult> testCases = new ArrayList<>();
        CaseResult caseResult = mock(CaseResult.class);
        when(caseResult.isPassed()).thenReturn(true);
        when(caseResult.isFailed()).thenReturn(false);
        when(caseResult.isSkipped()).thenReturn(false);
        testCases.add(caseResult);
        when(suiteResult.getCases()).thenReturn(testCases);

        when(testResultAction.getResult()).thenReturn(testResult);
        when(testResult.getSuites()).thenReturn(suiteResults);
        
        TestResults instance = TestResults.fromJUnitTestResults(testResultAction);
        
        assertNotNull(instance);
        assertEquals(1, instance.getTestSuites().size());
        assertEquals(1, instance.getPassedTestCaseCount());
        assertEquals(0, instance.getSkippedTestCaseCount());
        assertEquals(0, instance.getFailedTestCaseCount());
    }
    
}
