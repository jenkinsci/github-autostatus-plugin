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

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStages;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;      
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

/**
 *
 * @author jxpearce
 */

/**
 * GithubBuildStatusGraphListener watches builds, and provides status (pending,
 * error or success) to the github page for multibranch jobs
 * @author jxpearce@godaddy.com
 */
@Extension
public class GithubBuildStatusGraphListener implements GraphListener {

    @Override
    public void onNewHead(FlowNode fn) {
        try {
            if (isStage(fn)) {
                checkEnableBuildStatus(fn.getExecution());
            } else if (fn instanceof StepEndNode) {
                BuildStatusAction buildStatusAction = buildStatusActionFor(fn.getExecution());
                if (buildStatusAction == null) {
                    return;
                }
                String startId = ((StepEndNode)fn).getStartNode().getId();
                FlowNode startNode = fn.getExecution().getNode(startId);
                if (null == startNode) {
                    return;
                }
                LabelAction label = startNode.getAction(LabelAction.class);
                
                if (label != null) {
                    BuildStatus buildStatus = buildStatusAction.getBuildStatusForStage(label.getDisplayName());
                       if (buildStatus != null) {
                        ErrorAction errorAction = fn.getAction(ErrorAction.class);
                        // link to influxdb source
                        URL url = new URL( "http://10.33.178.125:8086/write?db=jenkins_db" );
                        int val = 1;
                        if(errorAction != null) {
                           val = 0;       
                        }
                        
                        String data = String.format("%s value=%d", label.getDisplayName().replaceAll("\\s", ""), val);
                        postData(data, url);
                        
                        buildStatus.setCommitState(errorAction == null ?
                                GHCommitState.SUCCESS :
                                GHCommitState.ERROR);
                    }
                }  
            }         
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Determines if a FlowNode describes a stage
     * @param node
     * @return true if it's a stage node; false otherwise
     */
    private static boolean isStage(FlowNode node){
        return node !=null && ((node.getAction(StageAction.class) != null)
            || (node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null));
    }
    
    private static void postData(String urlParameters, URL url) {
        try {    
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setDoOutput(true);
            Random rn = new Random();
            System.out.println(urlParameters);

            try (OutputStreamWriter writer =
                    new OutputStreamWriter(client.getOutputStream())) {
                writer.write(urlParameters);
            }
            catch(Exception e) {
               System.out.println("Exception" + e);
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    client.getInputStream()))) {
                String decodedString;
                while ((decodedString = in.readLine()) != null) {
                    System.out.println(decodedString);
                }
            }
            catch(Exception e) {
               System.out.println("Exception" + e);
            }
                
        }
        catch(IOException e) {
            System.out.println("Exception" + e);
        }
    }
    /**
     * Checks whether the current build meets our requirements for providing status,
     * and adds a BuildStatusAction to the build if so.
     * @param exec 
     */
    private static void checkEnableBuildStatus(FlowExecution exec) {        
        try {
            BuildStatusAction buildStatusAction = buildStatusActionFor(exec);
            
            if (null != buildStatusAction) {
                getLogger().log(Level.INFO, "BuildStatusAction set by previous step");
                return;
            }
            
            Run<?, ?> run = runFor(exec);
            if (null == run) {
                getLogger().log(Level.INFO, "Could not find Run - status will not be provided for this build");
                return;
            }
            getLogger().log(Level.INFO, "Processing build {0}", run.getId());
            ExecutionModelAction executionModelAction = run.getAction(ExecutionModelAction.class);
            if (null == executionModelAction) {
                getLogger().log(Level.INFO, "Could not find ExecutionModelAction - status will not be provided for this build");
                return;
            }
            ModelASTStages stages = executionModelAction.getStages();
            if (null == stages) {
                getLogger().log(Level.INFO, "Could not find ModelASTStages - status will not be provided for this build");
                return;
            }
            String commitSha = getCommitSha(run);
            if (null == commitSha) {
                getLogger().log(Level.INFO, "Could not find commit sha - status will not be provided for this build");
                return;
            }
            String targetUrl = DisplayURLProvider.get().getRunURL(run);
            
            GHRepository repo = getGHRepository(run, exec.getOwner().getListener());
            if (null == repo) {
                getLogger().log(Level.INFO, "Could not find commit GHRepository - status will not be provided for this build");
                return;
            }
            List<ModelASTStage> stageList = stages.getStages();
            if (null == stageList) {
                return;
            }

            getLogger().log(Level.INFO, "Providing build status for job");

            buildStatusAction = new BuildStatusAction(repo,
                    commitSha,
                    targetUrl,
                    convertList(stageList));
            
            run.addAction(buildStatusAction);        
        } catch (IOException ex) {
            try {
                exec.getOwner().getListener().getLogger().println(ex.toString());
            } catch (IOException ex1) {
                Logger.getLogger(GithubBuildStatusGraphListener.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(GithubBuildStatusGraphListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Converts a list of ModelAStage objects to a list of stage names
     * @param modelList list to convert
     * @return list of stage names
     */
    private static List<String> convertList(List<ModelASTStage> modelList) {
        ArrayList<String> result = new ArrayList<>();
        for (ModelASTStage stage : modelList) {
            result.add(stage.getName());
        }
        return result;
    }

    private static @CheckForNull BuildStatusAction buildStatusActionFor(FlowExecution exec) {
        BuildStatusAction buildStatusAction = null;
        Run<?, ?> run = runFor(exec);
        if (run != null) {
            buildStatusAction = run.getAction(BuildStatusAction.class);
        }
        return buildStatusAction;
    }

    private static @CheckForNull Run<?, ?> runFor(FlowExecution exec) {
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
    
        private static <T extends Credentials> T getCredentials(@Nonnull Class<T> type, @Nonnull String credentialsId, Item context) {
        return CredentialsMatchers.firstOrNull(lookupCredentials(
            type, context, ACL.SYSTEM,
            Collections.<DomainRequirement>emptyList()), CredentialsMatchers.allOf(
            CredentialsMatchers.withId(credentialsId),
            CredentialsMatchers.instanceOf(type)));
    }
    
    private static String getCommitSha(Run<?, ?> run) {
        String shaString = null;
        SCMRevisionAction scmRevisionAction = run.getAction(SCMRevisionAction.class);
        if (null == scmRevisionAction) {
            return null;
        }
        if (scmRevisionAction.getRevision() instanceof AbstractGitSCMSource.SCMRevisionImpl) {
            shaString = ((AbstractGitSCMSource.SCMRevisionImpl) scmRevisionAction.getRevision()).getHash();
        } else if (scmRevisionAction.getRevision() instanceof PullRequestSCMRevision) {
            shaString = ((PullRequestSCMRevision) scmRevisionAction.getRevision()).getPullHash();
        }
        
        return shaString;
    }
    
    private static GHRepository getGHRepository (Run<?, ?> run, TaskListener listener) throws IOException {
        ItemGroup parent = run.getParent().getParent();
        WorkflowMultiBranchProject project = null;
        if (parent instanceof WorkflowMultiBranchProject) {
            project = (WorkflowMultiBranchProject)parent;
        } 
        if (null == project) {
            getLogger().log(Level.INFO, "Project is not a multibranch project - status will not be provided for this build");
            return null;
        }
        GitHubSCMSource gitHubScmSource = null;
        SCMSource scmSource = project.getSCMSources().get(0);
        if (scmSource != null && scmSource instanceof GitHubSCMSource) {
            gitHubScmSource = (GitHubSCMSource)scmSource;
        }
        if (null == gitHubScmSource) {
            getLogger().log(Level.INFO, "Could not find githubSCMSource - status will not be provided for this build");
            return null;
        }
        String credentialsId = gitHubScmSource.getCredentialsId();
        if (null == credentialsId) {
            getLogger().log(Level.INFO, "Could not find credentials - status will not be provided for this build");
            return null;
        }
        listener.getLogger().println(credentialsId);
        String repoOwner = gitHubScmSource.getRepoOwner();
        String repository = gitHubScmSource.getRepository();
        String url = gitHubScmSource.getApiUri();
        if (null == url) {
            url = GitHubSCMSource.GITHUB_URL;
        }
        
        listener.getLogger().println(repoOwner);
        listener.getLogger().println(repository);
        listener.getLogger().println(url);

        getLogger().log(Level.INFO, "Repository is + ", repository);
        getLogger().log(Level.INFO, "Github API is + ", url);

        UsernamePasswordCredentials credentials = getCredentials(UsernamePasswordCredentials.class, credentialsId, run.getParent());        
        
        GitHubBuilder githubBuilder = new GitHubBuilder().withEndpoint(url);
        githubBuilder.withPassword(credentials.getUsername(), credentials.getPassword().getPlainText());
        
        GitHub github = githubBuilder.build();
        GHRepository repo = github.getUser(repoOwner).getRepository(repository);
        
        return repo;
    }
    
    private static Logger getLogger() {
        return Logger.getLogger(GithubBuildStatusGraphListener.class.getName());   
    }    
}
