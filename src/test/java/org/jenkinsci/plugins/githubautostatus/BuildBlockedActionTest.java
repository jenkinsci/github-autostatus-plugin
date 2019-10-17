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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildBlockedActionTest {

    public BuildBlockedActionTest() {
    }

    /**
     * Verifies default constructor sets blocked time
     */
    @Test
    public void testDefaultConstructorSetsStart() {
        BuildBlockedAction instance = new BuildBlockedAction();

        assertNotEquals(0, instance.getTimeBlocked());
    }

    /**
     * Verifies constructor overrides blocked time
     */
    @Test
    public void testDefaultConstructorOverrideStart() {
        BuildBlockedAction instance = new BuildBlockedAction(System.currentTimeMillis());

        assertNotEquals(0, instance.getTimeBlocked());
    }

    /**
     * Verifies release time can be set
     */
    @Test
    public void testGetSetTimeReleased() {
        long timeReleased = 123456;
        BuildBlockedAction instance = new BuildBlockedAction();
        instance.setTimeReleased(timeReleased);

        assertEquals(timeReleased, instance.getTimeReleased());
    }

}
