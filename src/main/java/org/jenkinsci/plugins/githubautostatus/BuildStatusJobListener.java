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

import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildState;

/**
 * Implements {@link hudson.model.listeners.RunListener} extension point to
 * provide job status information to subscribers as jobs complete.
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
@Extension
public class BuildStatusJobListener extends RunListener<Run<?, ?>> {

    /**
     * Sends final build status notification
     *
     * @param build the build
     * @param listener listener
     */
    @Override
    public void onCompleted(Run<?, ?> build, TaskListener listener) {

        BuildStatusAction statusAction = build.getAction(BuildStatusAction.class);
        log(Level.INFO, "Build Completed");
        if (statusAction != null) {
            Result result = build.getResult();
            statusAction.updateBuildStatusForJob(result == Result.SUCCESS
                    ? BuildState.CompletedSuccess
                    : BuildState.CompletedError,
                    build.getDuration(),
                    getBlockedTime(build));
        }
    }
    
    /**
     * Determines the amount of time a build spent in the blocked state.
     * @param build The build to check.
     * @return Time spent in the blocked state, in milliseconds.
     */
    protected long getBlockedTime(Run<?, ?> build) {
        BuildBlockedAction action = build.getAction(BuildBlockedAction.class);
        
        return action == null ? 0 : action.getTimeBlocked();
    }

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(GithubNotificationConfig.class.getName());
    }
}
