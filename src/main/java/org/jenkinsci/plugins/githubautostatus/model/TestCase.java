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

import hudson.tasks.junit.CaseResult;

/**
 *
 * @author jxpearce
 */
public class TestCase {

    private String name;
    private boolean failed;
    private boolean passed;
    private boolean skipped;

    public static TestCase fromCaseResult(CaseResult caseResult) {
        TestCase testCase = new TestCase();

        if (caseResult.isPassed()) {
            testCase.setPassed(true);
        }
        if (caseResult.isSkipped()) {
            testCase.setSkipped(true);
        }
        if (caseResult.isFailed()) {
            testCase.setFailed(true);
        }
        testCase.setName(caseResult.getFullName());
        return testCase;

    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
    
    public int getPassedCount() {
        return passed ? 1 : 0;
    }

    public int getSkippedCount() {
        return skipped ? 1 : 0;
    }

    public int getFailedCount() {
        return failed ? 1 : 0;
    }
}
