/*
 * The MIT License
 *
 * Copyright 2018 Jeff Pearce (jxpearce@godaddy.com).
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
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.QueueListener;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;

/**
 * Implementation of {@link hudson.model.queue.QueueListener} which keeps track
 * of time a pipeline build spent blocked.
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@Extension
public class BuildQueueListener extends QueueListener {

    public BuildQueueListener() {

    }

    /**
     * {@inheritDoc}
     * Adds {@link BuildBlockedAction} action to blocked pipeline builds to keep
     * track of the time spent blocked.
     */
    @Override
    public void onEnterBlocked(Queue.BlockedItem item) {
        if (!(item.task instanceof ExecutorStepExecution.PlaceholderTask)) {
            return;
        }
        Run run = ((ExecutorStepExecution.PlaceholderTask) item.task).run();

        if (run != null) {
            run.addOrReplaceAction(new BuildBlockedAction(System.currentTimeMillis()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLeaveBlocked(Queue.BlockedItem item) {
        if (!(item.task instanceof ExecutorStepExecution.PlaceholderTask)) {
            return;
        }
        Run run = ((ExecutorStepExecution.PlaceholderTask) item.task).run();

        if (run != null) {
            BuildBlockedAction action = run.getAction(BuildBlockedAction.class);
            if (action != null) {
                action.setTimeReleased(System.currentTimeMillis());
            }
        }
    }
}
