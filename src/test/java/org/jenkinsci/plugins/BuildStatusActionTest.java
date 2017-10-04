/*
 * The MIT License
 *
 * Copyright 2017 jxpearce.
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
package org.jenkinsci.plugins;

import java.io.IOException;
import java.util.ArrayList;
import org.jenkinsci.plugins.githubautostatus.BuildStatusAction;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import static org.mockito.Mockito.*;

/**
 *
 * @author jxpearce
 */
public class BuildStatusActionTest {
    
    public BuildStatusActionTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Verifies addBuildStatus calls createCommitStatus with a status of pending
     * @throws java.io.IOException
     */
    @Test
    public void testAddBuildStatus() throws IOException {
        String stageName = "Stage 1";
        GHRepository repository = mock(GHRepository.class);
      
        BuildStatusAction instance = new BuildStatusAction(repository, "sha", "targetUrl", new ArrayList<String>());
        instance.addBuildStatus(stageName);
        
        verify(repository).createCommitStatus("sha", GHCommitState.PENDING, "targetUrl", "Building stage", stageName);
    }

    /**
     * Verifies getBuildStatus with an invalid stage name returns build status
     * @throws java.io.IOException
     */
    @Test
    public void testValidGetBuildStatus() throws IOException {
        String stageName = "Stage 1";
        GHRepository repository = mock(GHRepository.class);
      
        BuildStatusAction instance = new BuildStatusAction(repository, "sha", "targetUrl", new ArrayList<String>());
        instance.addBuildStatus(stageName);
        
        assertNotNull(instance.getBuildStatusForStage(stageName));
    }
    
    /**
     * Verifies getBuildStatus with an invalid stage name returns null
     * @throws java.io.IOException
     */
    public void testInvalidGetBuildStatus() throws IOException {
        String stageName = "Stage 1";
        GHRepository repository = mock(GHRepository.class);
      
        BuildStatusAction instance = new BuildStatusAction(repository, "sha", "targetUrl", new ArrayList<String>());
        instance.addBuildStatus(stageName);
        
        assertNull(instance.getBuildStatusForStage("Stage 2"));
    }

}
