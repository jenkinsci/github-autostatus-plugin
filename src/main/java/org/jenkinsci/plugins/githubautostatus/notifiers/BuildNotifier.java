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

/**
 * A notification subscriber which can send build stats to a particular sink.
 * 
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
public interface BuildNotifier {

    /**
     * Determine whether notifier is enabled.
     *
     * @return true if enabled; false otherwise.
     */
    boolean isEnabled();

    /**
     * Send a state change, such as from Pending to Success or Pending to Error.
     *
     * @param jobName    the name of the job.
     * @param nodeName   the node that has changed.
     * @param buildState the new state. note: this is a stage result, should be
     *                   metric #3.
     */
    void notifyBuildState(String jobName, String nodeName, BuildState buildState);

    /**
     * Send a state change with timing info
     *
     * @param jobName      the name of the job
     * @param nodeName     the node that has changed
     * @param buildState   the new state
     * @param nodeDuration elapsed time for this node
     * This is #4 (Timer)
     */
    void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long nodeDuration);

    /**
     * Send a notification when the job is complete
     *
     * @param jobName         the name of the job
     * @param buildState      state indicating success or failure
     * @param buildDuration   the build duration
     * @param blockedDuration time build was blocked before running
     * This one will send #1 and #2
     */
    void notifyFinalBuildStatus(String jobName, BuildState buildState, long buildDuration, long blockedDuration);

    /**
     * Sends a notification for an error regardless of whether initial pending
     * status was sent. Useful for reporting errors for non-declarative pipelines
     * since they can happen outside of a stage.
     *
     * @param jobName  the name of the job
     * @param nodeName the name of the node that failed
     * Just send #1 type. i.e. notifyBuildState(jobName, nodeName, "failure");
     */
    void sendNonStageError(String jobName, String nodeName);
}
