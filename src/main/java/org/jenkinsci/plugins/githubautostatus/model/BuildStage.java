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
package org.jenkinsci.plugins.githubautostatus.model;

import com.google.gson.annotations.SerializedName;
import hudson.model.Run;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierConstants;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jxpearce
 */
public class BuildStage {

    @SerializedName("name")
    private String stageName;
    @SkipSerialisation
    private Map<String, Object> environment;
    @SerializedName("result")
    private BuildState buildState;
    private transient Run<?, ?> run;
    @SkipSerialisation
    private boolean isStage = true;
    private long time;
    private boolean passed;

    public BuildStage(String stageName) {
        this(stageName, new HashMap<>());
    }

    public BuildStage(String stageName, Map<String, Object> environment) {
        this(stageName, environment, BuildState.Pending);
    }

    public BuildStage(String stageName,
                      Map<String, Object> environment,
                      BuildState buildState) {
        this.stageName = stageName;
        this.environment = new HashMap(environment);
        Object timingInfo = this.environment.get(BuildNotifierConstants.STAGE_DURATION);
        this.time = timingInfo == null ? 0 : (long)timingInfo;
        this.buildState = buildState;
        this.passed = buildState != BuildState.CompletedError;
    }
    
    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public void addToEnvironment(String key, Object value) {
        environment.put(key, value);
        Object timingInfo = environment.get(BuildNotifierConstants.STAGE_DURATION);
        time = timingInfo == null ? 0 : (long)timingInfo;
    }
    
    public void addAllToEnvironment(Map<String, Object> environment) {
        environment.forEach(this::addToEnvironment);
    }

    public BuildState getBuildState() {
        return buildState;
    }

    public void setBuildState(BuildState buildState) {
        this.buildState = buildState;
        passed = buildState != BuildState.CompletedError;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public void setRun(Run<?, ?> run) {
        this.run = run;
    }
    
    public boolean isStage() {
        return this.isStage;
    }
    
    public void setIsStage(boolean isStage) {
        this.isStage = isStage;
    }

    public long getTime() {
        return time;
    }

    public boolean isPassed() {
        return passed;
    }
}
