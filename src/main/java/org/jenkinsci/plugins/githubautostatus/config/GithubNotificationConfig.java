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
package org.jenkinsci.plugins.githubautostatus.config;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.security.ACL;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision;
import org.jenkinsci.plugins.githubautostatus.BuildStatusConfig;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * Encapsulates the logic of determining GitHub configuration for a build.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class GithubNotificationConfig {

    private String shaString = "";
    private String repoOwner = "";
    private String branchName = "";
    private String repoName = "";
    private GHRepository repo;

    private transient Run<?, ?> run;

    protected GitHubBuilder githubBuilder;

    /**
     * Gets the SHA for the build
     *
     * @return the SHA for the build
     */
    public String getShaString() {
        return shaString;
    }

    /**
     * Gets the repo owner.
     *
     * @return the repo owner
     */
    public String getRepoOwner() {
        return repoOwner;
    }

    /**
     * Gets the name of the branch for the build.
     *
     * @return the name of the branch for the build
     */
    public String getBranchName() {
        return branchName;
    }

    /**
     * Gets the GitHub repo for the build.
     *
     * @return the GitHub repo for the build
     */
    public GHRepository getRepo() {
        return repo;
    }

    /**
     * Gets the name of the repo for the build.
     *
     * @return the name of the repo for the build
     */
    public String getRepoName() {
        return repoName;
    }

    /**
     * Constructs a config object from a Run object.
     *
     * @param run the build
     * @return the GitHub config object
     */
    public static @Nullable GithubNotificationConfig fromRun(Run<?, ?> run) {
        return GithubNotificationConfig.fromRun(run, new GitHubBuilder());
    }

    /**
     * Constructs a config object from a {@link Run} object and GitHub builder.
     *
     * @param run the build
     * @param githubBuilder the GitHub builder
     * @return the constructed config object
     */
    public static @Nullable GithubNotificationConfig fromRun(Run<?, ?> run, GitHubBuilder githubBuilder) {
        BuildStatusConfig buildStatusConfig = BuildStatusConfig.get();
        if (buildStatusConfig.getEnableGithub()) {
            try {
                GithubNotificationConfig result = new GithubNotificationConfig();
                result.githubBuilder = githubBuilder;
                result.run = run;
                if (!result.extractCommitSha(run)) {
                    return null;
                }
                if (!result.extractBranchInfo(run)) {
                    return null;
                }
                if (!result.extractGHRepositoryInfo(run)) {
                    return null;
                }
                return result;
            } catch (IOException ex) {
                log(Level.SEVERE, ex);
            }
        }
        return null;
    }

    /**
     * Creates a fresh GHRepository by re-resolving credentials from the Jenkins
     * credentials store. This is necessary because GitHub App tokens have a
     * non-configurable TTL of 60 minutes, and builds that run longer than that
     * will fail to update commit statuses with the original token.
     *
     * @return a fresh GHRepository, or null if credentials could not be resolved
     */
    public @Nullable GHRepository createRepository() {
        if (run == null) {
            return repo;
        }
        try {
            GitHubBuilder freshBuilder = new GitHubBuilder();
            GHRepository freshRepo = buildGHRepository(run, freshBuilder);
            if (freshRepo != null) {
                repo = freshRepo;
            }
            return freshRepo;
        } catch (IOException ex) {
            log(Level.SEVERE, ex);
            return null;
        }
    }

    /**
     * Extracts the SHA for the build.
     *
     * @param build the build
     * @return true if SHA was extracted; false if it could not be extracted
     */
    private Boolean extractCommitSha(Run<?, ?> build) {
        SCMRevisionAction scmRevisionAction = build.getAction(SCMRevisionAction.class);
        if (null == scmRevisionAction) {
            log(Level.INFO, "Could not find commit sha - status will not be provided for this build");
            return false;
        }
        SCMRevision revision = scmRevisionAction.getRevision();
        if (revision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
            this.shaString = ((AbstractGitSCMSource.SCMRevisionImpl) revision).getHash();
        } else if (revision instanceof PullRequestSCMRevision) {
            this.shaString = ((PullRequestSCMRevision) revision).getPullHash();
        }
        return true;
    }

    /**
     * Extracts the branch info from a build.
     *
     * @param build the build
     * @return true if branch info was extracted; false otherwise
     */
    private Boolean extractBranchInfo(Run<?, ?> build) {
        SCMRevisionAction scmRevisionAction = build.getAction(SCMRevisionAction.class);
        if (null == scmRevisionAction) {
            return false;
        }
        SCMRevision revision = scmRevisionAction.getRevision();
        if (revision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
            branchName = ((AbstractGitSCMSource.SCMRevisionImpl) revision).getHead().getName();
        } else if (revision instanceof PullRequestSCMRevision) {
            PullRequestSCMHead pullRequestSCMHead = (PullRequestSCMHead) ((PullRequestSCMRevision) revision).getHead();

            branchName = pullRequestSCMHead.getSourceBranch();
        }
        return true;
    }

    /**
     * Extracts the GHRepository from a build.
     *
     * @param build the build
     * @return true if the GHRepository was extracted; false otherwise
     * @throws IOException
     */
    private Boolean extractGHRepositoryInfo(Run<?, ?> build) throws IOException {
        ItemGroup parent = build.getParent().getParent();
        WorkflowMultiBranchProject project = null;
        if (parent instanceof WorkflowMultiBranchProject) {
            project = (WorkflowMultiBranchProject) parent;
        }
        if (null == project) {
            log(Level.INFO, "Project is not a multibranch project - status will not be provided for this build");
            return false;
        }
        GitHubSCMSource gitHubScmSource = null;
        SCMSource scmSource = project.getSCMSources().get(0);
        if (scmSource != null && scmSource instanceof GitHubSCMSource) {
            gitHubScmSource = (GitHubSCMSource) scmSource;
        }
        if (null == gitHubScmSource) {
            log(Level.INFO, "Could not find githubSCMSource - status will not be provided for this build");
            return false;
        }
        String credentialsId = gitHubScmSource.getCredentialsId();
        if (null == credentialsId) {
            log(Level.WARNING, "Could not find credentials - status will not be provided for this build");
            return false;
        }
        repoOwner = gitHubScmSource.getRepoOwner();
        repoName = gitHubScmSource.getRepository();

        repo = buildGHRepository(build, githubBuilder);

        return repo != null;
    }

    /**
     * Builds a GHRepository by resolving credentials from the Jenkins
     * credentials store and connecting to the GitHub API.
     *
     * @param build the build to resolve credentials for
     * @param builder the GitHubBuilder to use for building the connection
     * @return the GHRepository, or null if credentials could not be resolved
     * @throws IOException if there is a problem connecting to GitHub
     */
    private GHRepository buildGHRepository(Run<?, ?> build, GitHubBuilder builder) throws IOException {
        ItemGroup parent = build.getParent().getParent();
        WorkflowMultiBranchProject project = null;
        if (parent instanceof WorkflowMultiBranchProject) {
            project = (WorkflowMultiBranchProject) parent;
        }
        if (null == project) {
            return null;
        }
        GitHubSCMSource gitHubScmSource = null;
        SCMSource scmSource = project.getSCMSources().get(0);
        if (scmSource != null && scmSource instanceof GitHubSCMSource) {
            gitHubScmSource = (GitHubSCMSource) scmSource;
        }
        if (null == gitHubScmSource) {
            return null;
        }
        String credentialsId = gitHubScmSource.getCredentialsId();
        if (null == credentialsId) {
            return null;
        }
        String url = gitHubScmSource.getApiUri();
        String owner = gitHubScmSource.getRepoOwner();
        String name = gitHubScmSource.getRepository();

        String userName = null;
        String password = "";

        UsernamePasswordCredentials credentials = getCredentials(UsernamePasswordCredentials.class, credentialsId, build.getParent());
        if (credentials != null) {
            userName = credentials.getUsername();
            password = credentials.getPassword().getPlainText();
        } else {
            StringCredentials stringCredentials = getCredentials(StringCredentials.class, credentialsId, build.getParent());
            if (stringCredentials != null) {
                userName = stringCredentials.getId();
                password = stringCredentials.getSecret().getPlainText();
            }
        }
        if (userName == null) {
            log(Level.WARNING, "Could not resolve credentials - status will not be provided for this build");
            return null;
        }

        // I have no idea why, but we do get NPE in tests trying to
        // use githubBuilder.*() below even though withEndpoint()
        // returns "this" eventually and should never yield null.
        // Yet here we are. Still, it also changes "this" so we do
        // not really need the result of such assignments unless
        // we chain ghb.withX().withY() etc.
        GitHubBuilder ghbTmp = builder.withEndpoint(url);
        if (ghbTmp != null) {
            builder = ghbTmp;
        }
        builder.withPassword(userName, password);
        GitHub github = builder.build();
        return github.getUser(owner).getRepository(name);
    }

    private static <T extends Credentials> T getCredentials(@Nonnull Class<T> type, @Nonnull String credentialsId, Item context) {
        return CredentialsMatchers.firstOrNull(lookupCredentials(
                type, context, ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.allOf(
                CredentialsMatchers.withId(credentialsId),
                CredentialsMatchers.instanceOf(type)));
    }

    private static void log(Level level, Throwable exception) {
        getLogger().log(level, null, exception);
    }

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(GithubNotificationConfig.class.getName());
    }
}
