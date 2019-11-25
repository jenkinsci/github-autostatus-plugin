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

import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class TestSuite {

    private String name;
    private ArrayList<TestCase> testCases;
    private float duration;

    @SerializedName("passed")
    private int passedTestCaseCount;
    @SerializedName("skipped")
    private int skippedTestCaseCount;
    @SerializedName("failed")
    private int failedTestCaseCount;

    public TestSuite() {
        testCases = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public ArrayList<TestCase> getTestCases() {
        return testCases;
    }

    public void addTestCases(TestCase testCase) {
        passedTestCaseCount += testCase.getPassedCount();
        skippedTestCaseCount += testCase.getSkippedCount();
        failedTestCaseCount += testCase.getFailedCount();
        this.testCases.add(testCase);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestSuite)) return false;
        TestSuite suite = (TestSuite) o;
        return getPassedTestCaseCount() == suite.getPassedTestCaseCount() &&
                getSkippedTestCaseCount() == suite.getSkippedTestCaseCount() &&
                getFailedTestCaseCount() == suite.getFailedTestCaseCount() &&
                Objects.equals(getName(), suite.getName()) &&
                Objects.equals(getTestCases(), suite.getTestCases()) &&
                Objects.equals(getDuration(), suite.getDuration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTestCases(), getDuration(), getPassedTestCaseCount(), getSkippedTestCaseCount(), getFailedTestCaseCount());
    }
}
