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

import hudson.model.Result;
import hudson.model.Run;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierConstants;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class BuildStage {

    private String stageName;
    @SkipSerialisation
    private Map<String, Object> environment;
    private State buildState;
    private transient Run<?, ?> run;
    @SkipSerialisation
    private boolean isStage = true;
    private long duration;
    private boolean passed;

    public enum State {
        Pending,
        SkippedConditional,
        SkippedUnstable,
        SkippedFailure,
        CompletedSuccess,
        CompletedError,
        Unstable,
        NotBuilt,
        Aborted,
        Unknown;

        public static @Nonnull
        State fromResult(Result result) {
            switch (result.ordinal) {
                case 0:
                    return State.CompletedSuccess;
                case 1:
                    return Unstable;
                case 2:
                    return CompletedError;
                case 3:
                    return NotBuilt;
                case 4:
                    return Aborted;
                default:
                    return Unknown;
            }
        }
    }

    public BuildStage(String stageName) {
        this(stageName, new HashMap<>());
    }

    public BuildStage(String stageName, Map<String, Object> environment) {
        this(stageName, environment, State.Pending);
    }

    public BuildStage(String stageName,
                      Map<String, Object> environment,
                      State buildState) {
        this.stageName = stageName;
        this.environment = new HashMap<>(environment);
        Object timingInfo = this.environment.get(BuildNotifierConstants.STAGE_DURATION);
        this.duration = timingInfo == null ? 0 : (long) timingInfo;
        this.buildState = buildState;
        this.passed = buildState != State.CompletedError;
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
        duration = timingInfo == null ? 0 : (long) timingInfo;
    }

    public void addAllToEnvironment(Map<String, Object> environment) {
        environment.forEach(this::addToEnvironment);
    }

    public State getBuildState() {
        return buildState;
    }

    public void setBuildState(State buildState) {
        this.buildState = buildState;
        passed = buildState != State.CompletedError;
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

    public long getDuration() {
        return duration;
    }

    public boolean isPassed() {
        return passed;
    }
}
