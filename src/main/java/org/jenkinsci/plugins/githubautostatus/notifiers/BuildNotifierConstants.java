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

import java.util.Map;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildNotifierConstants {

    public static final String BLOCKED_DURATION = "BLOCKED_DURATION";
    public static final String BRANCH_NAME = "BRANCH_NAME";
    public static final String BUILD_OBJECT = "BUILD_OBJECT";
    public static final String COVERAGE_INFO = "COVERAGE_INFO";
    public static final String JOB_DURATION = "JOB_DURATION";
    public static final String JOB_NAME = "JOB_NAME";
    public static final String REPO_NAME = "REPO_NAME";
    public static final String REPO_OWNER = "REPO_OWNER";
    public static final String STAGE_DURATION = "STAGE_DURATION";
    public static final String TEST_CASE_INFO = "TEST_CASE_INFO";
    public static final String DEFAULT_STRING = "none";
    public static final long DEFAULT_LONG = 0;

    public static long getLong(Map<String, Object> map, String mapKey) {
        Object mapValue = map.get(mapKey);

        if (mapValue != null) {
            return (long) mapValue;
        }
        return BuildNotifierConstants.DEFAULT_LONG;
    }
}
