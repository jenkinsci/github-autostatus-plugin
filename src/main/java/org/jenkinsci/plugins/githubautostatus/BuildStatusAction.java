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
package org.jenkinsci.plugins.githubautostatus;

import hudson.model.InvisibleAction;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

/**
 *
 * @author jxpearce
 */
public class BuildStatusAction extends InvisibleAction {
    
    public BuildStatusAction(GHRepository repository, 
            String shaString, 
            String targetUrl, 
            List<String> stageList) throws IOException {
        this.repository = repository;
        this.shaString = shaString;
        this.targetUrl = targetUrl;
        this.buildStatuses = new HashMap<>();
        for (String stageName : stageList) {
            addBuildStatus(stageName);
        }            
    }
    
    private final HashMap<String, BuildStatus> buildStatuses;
    
    private final GHRepository repository;
    
    private final String shaString;
    
    private final String targetUrl;
    
    public void addBuildStatus(String stageName) {
        try {
            BuildStatus buildStatus = new BuildStatus(repository, shaString, targetUrl, stageName);
            buildStatus.setCommitState(GHCommitState.PENDING);        
            buildStatuses. put(buildStatus.getContext(), buildStatus);
        } catch (IOException ex) {
            Logger.getLogger(BuildStatusAction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public BuildStatus getBuildStatusForStage(String stageName) {
        return buildStatuses.get(stageName);
    }
}
