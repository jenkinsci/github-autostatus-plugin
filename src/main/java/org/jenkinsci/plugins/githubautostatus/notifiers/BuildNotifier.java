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

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import org.jenkinsci.plugins.githubautostatus.BuildStageModel;
import org.jenkinsci.plugins.githubautostatus.BuildState;

import java.util.Map;

/**
 * A notification subscriber which can send build stats to a particular sink.
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
public abstract class BuildNotifier implements ExtensionPoint {
    
    /**
     * Determine whether notifier is enabled.
     *
     * @return true if enabled; false otherwise.
     */
    abstract public boolean isEnabled();

    /**
     * Send a state change, such as from Pending to Success or Pending to Error.
     *

     * @param jobName the name of the job
     * @param stageItem stage item
     */
    abstract public void notifyBuildStageStatus(String jobName, BuildStageModel stageItem);

    /**
     * Send a notification when the job is complete
     *
     * @param buildState state indicating success or failure
     * @param parameters build parameters
     */
    abstract public void notifyFinalBuildStatus(BuildState buildState, Map<String, Object> parameters);

    public static ExtensionList<BuildNotifier> all() {
        return ExtensionList.lookup(BuildNotifier.class);
    }
}
