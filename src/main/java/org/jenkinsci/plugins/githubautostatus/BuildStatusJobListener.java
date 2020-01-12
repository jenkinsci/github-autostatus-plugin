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

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.plugins.cobertura.CoberturaBuildAction;
import hudson.plugins.jacoco.JacocoBuildAction;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.githubautostatus.config.HttpNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.config.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.model.CodeCoverage;
import org.jenkinsci.plugins.githubautostatus.model.TestResults;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierConstants;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements {@link RunListener} extension point to
 * provide job status information to subscribers as jobs complete.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@Extension
public class BuildStatusJobListener extends RunListener<Run<?, ?>> {

    /**
     * Sends final build status notification.
     *
     * @param build the build
     * @param listener listener
     */
    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        if (build instanceof FreeStyleBuild) {
            enableFreeStyleBuild((FreeStyleBuild) build);
        }
        BuildStatusAction statusAction = build.getAction(BuildStatusAction.class);
        if (statusAction != null) {
            Map<String, Object> parameters = getParameters(build);
            parameters.put(BuildNotifierConstants.BUILD_OBJECT, build);
            parameters.put(BuildNotifierConstants.COVERAGE_INFO, getCoverageData(build));
            parameters.put(BuildNotifierConstants.JOB_DURATION, build.getDuration());
            parameters.put(BuildNotifierConstants.TEST_CASE_INFO, getTestData(build));
            parameters.put(BuildNotifierConstants.BLOCKED_DURATION, getBlockedTime(build));
            parameters.put(BuildNotifierConstants.JOB_NAME, statusAction.getJobName());
            parameters.put(BuildNotifierConstants.REPO_NAME, statusAction.getRepoName());
            parameters.put(BuildNotifierConstants.BRANCH_NAME, statusAction.getBranchName());

            Result result = build.getResult();
            if (result == null) {
                log(Level.WARNING, String.format("Could not get result of build \"%s\". Notifications are ignored.", statusAction.getRepoName()));
                return;
            }
            statusAction.updateBuildStatusForJob(BuildStage.State.fromResult(result), parameters);
        }
    }

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        super.onStarted(run, listener);
    }

    /**
     * Sets the build status action for a freestyle build, so we can send a few basic stats.
     *
     * @param build the build
     */
    private void enableFreeStyleBuild(FreeStyleBuild build) {
        String repoOwner = "";
        String repoName = build.getProject().getName();
        String branchName = "";

        BuildStatusAction buildStatusAction = BuildStatusAction.newAction(build, null, Collections.emptyList());
        build.addAction(buildStatusAction);

        buildStatusAction.setRepoOwner(repoOwner);
        buildStatusAction.setRepoName(repoName);
        buildStatusAction.setBranchName(branchName);
        buildStatusAction.addInfluxDbNotifier(InfluxDbNotifierConfig.fromGlobalConfig(repoOwner, repoName, branchName));
        buildStatusAction.addHttpNotifier(HttpNotifierConfig.fromGlobalConfig(repoOwner, repoName, branchName));
    }

    /**
     * Creates a map containing all job parameters.
     *
     * @param build the build
     * @return map containing parameters
     */
    private Map<String, Object> getParameters(Run<?, ?> build) {
        HashMap<String, Object> result = new HashMap<String, Object>();

        ParametersAction parametersAction = build.getAction(ParametersAction.class);

        if (parametersAction != null) {
            for (ParameterValue parameterValue : parametersAction.getAllParameters()) {
                result.put(parameterValue.getName(), parameterValue.getValue());
            }
        }

        return result;
    }

    /**
     * Gets code coverage from the build, if present.
     *
     * If both Cobertura and JaCoCo results are available, JaCoCo will take precedence.
     *
     * @param build the build
     * @return code coverage information
     */
    private CodeCoverage getCoverageData(Run<?, ?> build) {
        CodeCoverage results = null;
        CoberturaBuildAction coberturaAction = build.getAction(CoberturaBuildAction.class);
        JacocoBuildAction jacocoBuildAction = build.getAction(JacocoBuildAction.class);

        if (coberturaAction != null) {
            results = CodeCoverage.fromCobertura(coberturaAction);
        } else if (jacocoBuildAction != null) {
            results = CodeCoverage.fromJacoco(jacocoBuildAction);
        }

        return results;
    }

    /**
     * Gets test results from the build, if present.
     *
     * @param build the build
     * @return test results
     */
    private TestResults getTestData(Run<?, ?> build) {
        TestResultAction testResultAction = build.getAction(TestResultAction.class);

        return TestResults.fromJUnitTestResults(testResultAction);
    }

    /**
     * Determines the amount of time a build spent in the blocked state.
     *
     * @param build the build
     * @return time spent in the blocked state, in milliseconds
     */
    private long getBlockedTime(Run<?, ?> build) {
        BuildBlockedAction action = build.getAction(BuildBlockedAction.class);

        return action == null ? 0 : action.getTimeBlocked();
    }

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(BuildStatusJobListener.class.getName());
    }
}
