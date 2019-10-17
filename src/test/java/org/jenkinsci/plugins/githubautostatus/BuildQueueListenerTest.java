/*
 * The MIT License
 *
 * Copyright 2019 jxpearce.
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

import hudson.model.Queue.BlockedItem;
import hudson.model.Queue.NonBlockingTask;
import hudson.model.Run;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BlockedItem.class, ExecutorStepExecution.PlaceholderTask.class})
@PowerMockIgnore({"javax.crypto.*"})

public class BuildQueueListenerTest {

    public BuildQueueListenerTest() {
    }

    @Test
    public void testEnterBlockedPlaceHolder() throws Exception {
        
        Run run = mock(Run.class);
        
        ExecutorStepExecution.PlaceholderTask task = mock(ExecutorStepExecution.PlaceholderTask.class);
        when(task.run()).thenReturn(run);

        BlockedItem item = mock(BlockedItem.class);
        
        setFinal(item, BlockedItem.class.getField("task"), task);

        BuildQueueListener buildQueueListener = new BuildQueueListener();
        
        buildQueueListener.onEnterBlocked(item);
        verify(run).addOrReplaceAction(any());    
    }

    @Test
    public void testEnterBlockedNotPlaceHolder() throws Exception {
        
        Run run = mock(Run.class);

        NonBlockingTask task = mock(NonBlockingTask.class);

        BlockedItem item = mock(BlockedItem.class);
        
        setFinal(item, BlockedItem.class.getField("task"), task);

        BuildQueueListener buildQueueListener = new BuildQueueListener();
        
        buildQueueListener.onEnterBlocked(item);
        
        // No verification possible, other than not throwing an exception
    }

    @Test
    public void testLeaveBlockedNotPlaceholderTask() throws Exception {
        
        NonBlockingTask task = mock(NonBlockingTask.class);

        BlockedItem item = mock(BlockedItem.class);
        
        setFinal(item, BlockedItem.class.getField("task"), task);

        BuildQueueListener buildQueueListener = new BuildQueueListener();
        
        buildQueueListener.onLeaveBlocked(item);

        // No verification possible, other than not throwing an exception
   
    }

    @Test
    public void testLeaveBlockedUpdatesAction() throws Exception {
        
        Run run = mock(Run.class);
        BuildBlockedAction buildBlockedAction = mock(BuildBlockedAction.class);
        when(run.getAction(BuildBlockedAction.class)).thenReturn(buildBlockedAction);
        
        ExecutorStepExecution.PlaceholderTask task = mock(ExecutorStepExecution.PlaceholderTask.class);
        when(task.run()).thenReturn(run);

        BlockedItem item = mock(BlockedItem.class);
        
        setFinal(item, BlockedItem.class.getField("task"), task);

        BuildQueueListener buildQueueListener = new BuildQueueListener();
        
        buildQueueListener.onLeaveBlocked(item);
        verify(buildBlockedAction).setTimeReleased(anyLong());
    }
    
    static void setFinal(Object object, Field field, Object newValue) throws Exception {
        field.setAccessible(true);        
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(object, newValue);
    }

}
