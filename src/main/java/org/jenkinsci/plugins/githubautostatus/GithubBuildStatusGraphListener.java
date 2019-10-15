/*
 * The MIT License
 *
 * Copyright 2017 Jeff Pearce (jxpearce@godaddy.com).
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
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.pipeline.StageStatus;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.*;
import org.jenkinsci.plugins.workflow.actions.*;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * GraphListener implementation which provides status (pending, error or
 * success) and timing information for each stage in a build.
 *
 * @author Jeff Pearce (GitHub jeffpeare)
 */
@Extension
public class GithubBuildStatusGraphListener implements GraphListener {

    /**
     * Evaluate if we can provide stats on a node.
     * 
     * @param fn a node in workflow
     */
    @Override
    public void onNewHead(FlowNode fn) {
        try {
            if (isStage(fn)) {
                checkEnableBuildStatus(fn);
            } else if (fn instanceof StepAtomNode) {

                // We don't need to look at atom nodes for declarative pipeline jobs, because
                // they have a nice model containing all the stages
                if (isDeclarativePipelineJob(fn)) {
                    return;
                }

                ErrorAction errorAction = fn.getError();
                String nodeName = null;

                if (errorAction == null) {
                    return;
                }

                List<? extends FlowNode> enclosingBlocks = fn.getEnclosingBlocks();
                boolean isInStage = false;

                for (FlowNode encosingNode : enclosingBlocks) {
                    if (isStage(encosingNode)) {
                        isInStage = true;
                    }
                }

                if (isInStage) {
                    return;
                }

                // We have a non-declarative atom that isn't in a stage, which has failed.
                // Since normal processing is via stages, we'd normally miss this failure;
                // send an out of band error notification to make sure it's recordded by any
                // interested notifiers
                checkEnableBuildStatus(fn);
                BuildStatusAction buildStatusAction = buildStatusActionFor(fn.getExecution());
                if (buildStatusAction == null) {
                    return;
                }

                buildStatusAction.sendNonStageError(fn.getDisplayName());

            } else if (fn instanceof StepEndNode) {
                BuildStatusAction buildStatusAction = buildStatusActionFor(fn.getExecution());
                if (buildStatusAction == null) {
                    return;
                }

                String startId = ((StepEndNode) fn).getStartNode().getId();
                FlowNode startNode = fn.getExecution().getNode(startId);
                if (null == startNode) {
                    return;
                }

                ErrorAction errorAction = fn.getError();
                String nodeName = null;

                long time = getTime(startNode, fn);
                LabelAction label = startNode.getAction(LabelAction.class);

                if (label != null) {
                    nodeName = label.getDisplayName();
                } else if (null != errorAction && startNode instanceof StepStartNode) {
                    nodeName = ((StepStartNode) startNode).getStepName();
                }

                if (nodeName != null) {
                    BuildStage.State buildState = buildStateForStage(startNode, errorAction);
                    buildStatusAction.updateBuildStatusForStage(nodeName, buildState, time);
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Determines the appropriate state for a stage
     *
     * @param flowNode The stage start node
     * @param errorAction The error action from the stage end node
     * @return Stage state
     */
    static BuildStage.State buildStateForStage(FlowNode flowNode, ErrorAction errorAction) {
        BuildStage.State buildState = errorAction == null ? BuildStage.State.CompletedSuccess : BuildStage.State.CompletedError;
        TagsAction tags = flowNode.getAction(TagsAction.class);
        if (tags != null) {
            String status = tags.getTagValue(StageStatus.TAG_NAME);
            if (status != null) {
                if (status.equals(StageStatus.getSkippedForFailure())) {
                    return BuildStage.State.SkippedFailure;
                } else if (status.equals(StageStatus.getSkippedForUnstable())) {
                    return BuildStage.State.SkippedUnstable;
                } else if (status.equals(StageStatus.getSkippedForConditional())) {
                    return BuildStage.State.SkippedConditional;
                } else if (status.equals(StageStatus.getFailedAndContinued())) {
                    return BuildStage.State.CompletedError;
                }
            }
        }
        return buildState;
    }

    /**
     * Get the execution time of a block defined by startNode and endNode
     * 
     * @param startNode startNode of a block
     * @param endNode endNode of a block
     * @return Execution time of the block
     */
    static long getTime(FlowNode startNode, FlowNode endNode) {
        TimingAction startTime = startNode.getAction(TimingAction.class);
        TimingAction endTime = endNode.getAction(TimingAction.class);

        if (startTime != null && endTime != null) {
            return endTime.getStartTime() - startTime.getStartTime();
        }
        return 0;
    }

    /**
     * Determines if a FlowNode describes a stage
     *
     * Note: this check is copied from PipelineNodeUtil.java in blueocean-plugin
     *
     * @param node node of a workflow
     * @return true if it's a stage node; false otherwise
     */
    private static boolean isStage(FlowNode node) {
        return node != null && ((node.getAction(StageAction.class) != null)
                || (node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null));
    }

    /**
     * Checks whether the current build meets our requirements for providing
     * status, and adds a BuildStatusAction to the build if so.
     *
     * @param flowNode node of a workflow
     */
    private static void checkEnableBuildStatus(FlowNode flowNode) {
        FlowExecution exec = flowNode.getExecution();
        try {
            BuildStatusAction buildStatusAction = buildStatusActionFor(exec);

            Run<?, ?> run = runFor(exec);
            if (null == run) {
                log(Level.INFO, "Could not find Run - status will not be provided for this build");
                return;
            } else {
                log(Level.INFO, "Processing build %s", run.getFullDisplayName());
            }

            // Declarative pipeline jobs come with a nice execution model, which allows you
            // to get all of the stages at once at the beginning of the job.
            // Older scripted pipeline jobs do not, so we have to add them one at a 
            // time as we discover them.
            List<BuildStage> stageNames = getDeclarativeStages(run);
            boolean isDeclarativePipeline = stageNames != null;

            String targetUrl;
            try {
                targetUrl = DisplayURLProvider.get().getRunURL(run);
            } catch (Exception e) {
                targetUrl = "";
            }

            if (isDeclarativePipeline && buildStatusAction != null) {
                buildStatusAction.connectNotifiers(run, targetUrl);
                return;
            }
            if (stageNames == null) {
                ArrayList<BuildStage> stageNameList = new ArrayList<>();
                stageNameList.add(new BuildStage(flowNode.getDisplayName()));
                stageNames = stageNameList;
            }

            if (buildStatusAction == null) {
                buildStatusAction = new BuildStatusAction(run, targetUrl, stageNames);
                buildStatusAction.setIsDeclarativePipeline(isDeclarativePipeline);

                run.addAction(buildStatusAction);
            } else {
                buildStatusAction.addBuildStatus(flowNode.getDisplayName());
            }
        } catch (Exception ex) {
            try {
                exec.getOwner().getListener().getLogger().println(ex.toString());
            } catch (IOException ex1) {
                Logger.getLogger(GithubBuildStatusGraphListener.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(GithubBuildStatusGraphListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Determines if the node belongs to a declarative pipeline
     * 
     * @param fn node of a workflow
     * @return true/false
     */
    private static boolean isDeclarativePipelineJob(FlowNode fn) {
        Run<?, ?> run = runFor(fn.getExecution());
        if (run == null) {
            return false;
        }
        return getDeclarativeStages(run) != null;

    }

    /**
     * Get a list of stages in a declarative pipeline
     * 
     * @param run a particular run of a job
     * @return a list of stage names
     */
    protected static List<BuildStage> getDeclarativeStages(Run<?, ?> run) {
        ExecutionModelAction executionModelAction = run.getAction(ExecutionModelAction.class);
        if (null == executionModelAction) {
            return null;
        }
        ModelASTStages stages = executionModelAction.getStages();
        if (null == stages) {
            return null;
        }
        List<ModelASTStage> stageList = stages.getStages();
        if (null == stageList) {
            return null;
        }
        return convertList(stageList);
    }

    /**
     * Converts a list of ModelAStage objects to a list of stage names
     *
     * @param modelList list to convert
     * @return list of stage names
     */
    private static List<BuildStage> convertList(List<ModelASTStage> modelList) {
        ArrayList<BuildStage> result = new ArrayList<>();
        for (ModelASTStage stage : modelList) {
            HashMap<String, Object> environmentVariables = new HashMap<String, Object>();
            ModelASTEnvironment modelEnvironment = stage.getEnvironment();
            if (modelEnvironment != null) {
                stage.getEnvironment().getVariables().forEach((key, value) -> {
                    String groovyValue = value.toGroovy();
                    if (groovyValue.startsWith("'")) {
                        groovyValue = groovyValue.substring(1);
                    }
                    if (groovyValue.endsWith("'")) {
                        groovyValue = groovyValue.substring(0, groovyValue.length() - 1);
                    }
                    environmentVariables.put(key.getKey(), groovyValue);
                });
            }
            ModelASTOptions options = stage.getOptions();
            if (options != null) {
                for (ModelASTOption option : options.getOptions()) {
                    for (ModelASTMethodArg arg : option.getArgs()) {
                        if (arg instanceof ModelASTKeyValueOrMethodCallPair) {
                            ModelASTKeyValueOrMethodCallPair arg2 = (ModelASTKeyValueOrMethodCallPair) arg;
                            JSONObject value = (JSONObject) arg2.getValue().toJSON();

                            environmentVariables.put(String.format("%s.%s", option.getName(), arg2.getKey().getKey()),
                                    value.get("value"));
                        }
                    }
                }
            }

            for (String stageName : getAllStageNames(stage)) {
                result.add(new BuildStage(stageName, environmentVariables));
            }
        }
        return result;
    }

    /**
     * Get the BuildStatusAction object for the specified executing workflow
     *
     * Returns a list containing the stage name and names of all nested stages.
     *
     * @param stage The ModelASTStage object
     * @return List of stage names
     */
    private static List<String> getAllStageNames(ModelASTStage stage) {
        List<String> stageNames = new ArrayList<>();
        stageNames.add(stage.getName());
        List<ModelASTStage> stageList = null;
        if (stage.getStages() != null) {
            stageList = stage.getStages().getStages();
        } else {
            stageList = stage.getParallelContent();
        }
        if (stageList != null) {
            for (ModelASTStage innerStage : stageList) {
                stageNames.addAll(getAllStageNames(innerStage));
            }
        }
        return stageNames;
    }

    private static @CheckForNull
    BuildStatusAction buildStatusActionFor(FlowExecution exec) {
        BuildStatusAction buildStatusAction = null;
        Run<?, ?> run = runFor(exec);
        if (run != null) {
            buildStatusAction = run.getAction(BuildStatusAction.class);
        }
        return buildStatusAction;
    }

    /**
     * Get the jenkins run object of the specified executing workflow
     * 
     * @param exec execution of a workflow
     * @return jenkins run object of a job
     */
    private static @CheckForNull
    Run<?, ?> runFor(FlowExecution exec) {
        Queue.Executable executable;
        try {
            executable = exec.getOwner().getExecutable();
        } catch (IOException x) {
            getLogger().log(Level.WARNING, null, x);
            return null;
        }
        if (executable instanceof Run) {
            return (Run<?, ?>) executable;
        } else {
            return null;
        }
    }

    /**
     * Print to stdout or stderr
     * 
     * @param level INFO/WARNING/ERROR
     * @param format String that formats the log
     * @param args arguments for the formated log string
     */
    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    /**
     * Get the logger for the listener
     * 
     * @return logger object
     */
    private static Logger getLogger() {
        return Logger.getLogger(GithubBuildStatusGraphListener.class.getName());
    }
}
