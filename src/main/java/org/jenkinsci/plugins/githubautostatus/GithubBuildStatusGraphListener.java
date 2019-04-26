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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.githubautostatus.notifiers.BuildState;
import org.jenkinsci.plugins.pipeline.StageStatus;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStages;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * GraphListener implementation which provides status (pending, error or success) 
 * and timing information for each stage in a build.
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
@Extension
public class GithubBuildStatusGraphListener implements GraphListener {

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
                    BuildState buildState = buildStateForStage(startNode, errorAction);
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
     * @return Build state
     */
    BuildState buildStateForStage(FlowNode flowNode, ErrorAction errorAction) {
        BuildState buildState = errorAction == null ? BuildState.CompletedSuccess : BuildState.CompletedError;;
        TagsAction tags = flowNode.getAction(TagsAction.class);
        if (tags != null) {
            String status = tags.getTagValue(StageStatus.TAG_NAME);
            if (status != null) {
                if (status.equals(StageStatus.getSkippedForFailure())) {
                    return BuildState.SkippedFailure;
                } else if (status.equals(StageStatus.getSkippedForUnstable())) {
                    return BuildState.SkippedUnstable;
                } else if (status.equals(StageStatus.getSkippedForConditional())) {
                    return BuildState.SkippedConditional;
                } else if (status.equals(StageStatus.getFailedAndContinued())) {
                    return BuildState.CompletedError;
                }

            }
        }
        return buildState;
    }

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
     * @param node
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
     * @param exec
     */
    private static void checkEnableBuildStatus(FlowNode flowNode) {
        FlowExecution exec = flowNode.getExecution();
        try {
            BuildStatusAction buildStatusAction = buildStatusActionFor(exec);

            Run<?, ?> run = runFor(exec);
            if (null == run) {
                log(Level.INFO, "Could not find Run - status will not be provided for this build");
                return;
            }
            log(Level.INFO, "Processing build %s", run.getFullDisplayName());

            // Declarative pipeline jobs come with a nice execution model, which allows you
            // to get all of the stages at once at the beginning of the kob. 
            // Older scripted pipeline jobs do not, so we have to add them one at a 
            // time as we discover them.
            List<String> stageNames = getDeclarativeStages(run);
            boolean isDeclarativePipeline = stageNames != null;

            if (isDeclarativePipeline && buildStatusAction != null) {
                return;
            }
            if (stageNames == null) {
                stageNames = Arrays.asList(flowNode.getDisplayName());
            }

            String targetUrl;
            try {
                targetUrl = DisplayURLProvider.get().getRunURL(run);
            } catch (Exception e) {
                targetUrl = "";
            }

            if (buildStatusAction == null) {
                buildStatusAction = new BuildStatusAction(run.getExternalizableId(), targetUrl, stageNames);
                buildStatusAction.setIsDeclarativePipeline(isDeclarativePipeline);

                String repoOwner = "";
                String repoName = "";
                String branchName = "";
                GithubNotificationConfig githubConfig = GithubNotificationConfig.fromRun(run, exec.getOwner().getListener());
                if (githubConfig != null) {
                    buildStatusAction.addGithubNofifier(githubConfig);
                    repoOwner = githubConfig.getRepoOwner();
                    repoName = githubConfig.getRepoName();
                    branchName = githubConfig.getBranchName();
                } else {
                    if (run instanceof WorkflowRun) {
                        repoName = run.getParent().getDisplayName();
                        repoOwner = run.getParent().getParent().getFullName();
                    }
                }
                buildStatusAction.addInfluxDbNotifier(
                        InfluxDbNotifierConfig.fromGlobalConfig(repoOwner, repoName, branchName));
                run.addAction(buildStatusAction);
            } else {
                buildStatusAction.addBuildStatus(flowNode.getDisplayName());
            }
        } catch (IOException ex) {
            try {
                exec.getOwner().getListener().getLogger().println(ex.toString());
            } catch (IOException ex1) {
                Logger.getLogger(GithubBuildStatusGraphListener.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(GithubBuildStatusGraphListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static boolean isDeclarativePipelineJob(FlowNode fn) {
        Run<?, ?> run = runFor(fn.getExecution());
        if (run == null) {
            return false;
        }
        return getDeclarativeStages(run) != null;

    }

    protected static List<String> getDeclarativeStages(Run<?, ?> run) {
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
    private static List<String> convertList(List<ModelASTStage> modelList) {
        ArrayList<String> result = new ArrayList<>();
        for (ModelASTStage stage : modelList) {
            result.addAll(getAllStageNames(stage));
        }
        return result;
    }

    /**
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
        for (ModelASTStage innerStage : stageList) {
            stageNames.addAll(getAllStageNames(innerStage));
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

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(GithubBuildStatusGraphListener.class.getName());
    }
}
