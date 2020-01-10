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
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;

import java.util.Map;

/**
 * A notification subscriber which can send build stats to a particular sink.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public abstract class BuildNotifier implements ExtensionPoint {

    /**
     * Establishing a default long for use in getLong.
     */
    protected final long DEFAULT_LONG = 0;

    /**
     * Establishing a default string for use in notifiers.
     */
    protected final String DEFAULT_STRING = "none";

    /**
     * Determines whether this notifier is enabled.
     *
     * @return true if enabled; false otherwise
     */
    abstract public boolean isEnabled();

    /**
     * Sends a state change with timing info.
     *
     * @param jobName the name of the job
     * @param stageItem stage item
     */
    abstract public void notifyBuildStageStatus(String jobName, BuildStage stageItem);

    /**
     * Sends a notification when a job is complete.
     *
     * @param buildState state indicating success or failure
     * @param parameters build parameters
     */
    abstract public void notifyFinalBuildStatus(BuildStage.State buildState, Map<String, Object> parameters);

    /**
     * Get whether the notifier wants to know about errors that happen outside of a stage.
     *
     * @return whether the notifier wants to know about errors that happen outside of a stage
     */
    public boolean wantsOutOfStageErrors() {
        return false;
    }

    public static ExtensionList<BuildNotifier> all() {
        return ExtensionList.lookup(BuildNotifier.class);
    }

    public long getLong(Map<String, Object> map, String mapKey) {
        Object mapValue = map.get(mapKey);

        if (mapValue != null) {
            return (long) mapValue;
        }
        return DEFAULT_LONG;
    }
}
