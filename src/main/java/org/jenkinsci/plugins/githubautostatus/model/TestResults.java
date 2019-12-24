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
package org.jenkinsci.plugins.githubautostatus.model;

import com.google.gson.annotations.SerializedName;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;
import java.util.ArrayList;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class TestResults {

    @SerializedName("passed")
    private int passedTestCaseCount;
    @SerializedName("skipped")
    private int skippedTestCaseCount;
    @SerializedName("failed")
    private int failedTestCaseCount;

    @SerializedName("suites")
    private ArrayList<TestSuite> testSuites;

    public void setTestSuites(ArrayList<TestSuite> testSuites) {
        this.testSuites = testSuites;
    }

    public TestResults() {
        testSuites = new ArrayList<>();
    }

    public static TestResults fromJUnitTestResults(@Nullable TestResultAction testResultAction) {

        if (testResultAction == null) {
            return null;
        }
        TestResults testResults = new TestResults();
        for (SuiteResult suiteResult : testResultAction.getResult().getSuites()) {
            TestSuite testSuite = new TestSuite();
            testSuite.setName(suiteResult.getName());
            testSuite.setDuration(suiteResult.getDuration());

            for (CaseResult caseResult : suiteResult.getCases()) {

                TestCase testCase = TestCase.fromCaseResult(caseResult);
                testResults.passedTestCaseCount += testCase.getPassedCount();
                testResults.skippedTestCaseCount += testCase.getSkippedCount();
                testResults.failedTestCaseCount += testCase.getFailedCount();
                testSuite.addTestCases(testCase);
            }
            testResults.testSuites.add(testSuite);
        }
        return testResults;
    }

    public int getPassedTestCaseCount() {
        return passedTestCaseCount;
    }

    public void setPassedTestCaseCount(int passedTestCaseCount) {
        this.passedTestCaseCount = passedTestCaseCount;
    }

    public int getSkippedTestCaseCount() {
        return skippedTestCaseCount;
    }

    public void setSkippedTestCaseCount(int skippedTestCaseCount) {
        this.skippedTestCaseCount = skippedTestCaseCount;
    }

    public int getFailedTestCaseCount() {
        return failedTestCaseCount;
    }

    public void setFailedTestCaseCount(int failedTestCaseCount) {
        this.failedTestCaseCount = failedTestCaseCount;
    }

    public ArrayList<TestSuite> getTestSuites() {
        return testSuites;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestResults)) return false;
        TestResults that = (TestResults) o;
        return getPassedTestCaseCount() == that.getPassedTestCaseCount() &&
                getSkippedTestCaseCount() == that.getSkippedTestCaseCount() &&
                getFailedTestCaseCount() == that.getFailedTestCaseCount() &&
                Objects.equals(getTestSuites(), that.getTestSuites());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPassedTestCaseCount(), getSkippedTestCaseCount(), getFailedTestCaseCount(), getTestSuites());
    }
}
