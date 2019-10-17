package org.jenkinsci.plugins.githubautostatus.config;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.google.common.base.Strings;
import org.jenkinsci.plugins.githubautostatus.BuildStatusConfig;

import javax.annotation.CheckForNull;

/**
 * Encapsulates the logic of determining HTTP notifier configuration for a build.
 *
 * @author An Nguyen (nthienan.it@gmail.com)
 */
public class HttpNotifierConfig extends AbstractNotifierConfig {

    private String repoOwner;
    private String repoName;
    private String branchName;
    private String httpEndpoint;
    private String httpCredentialsId;
    private boolean httpVerifySSL;

    /**
     * Gets the repo owner.
     *
     * @return the repo owner
     */
    public String getRepoOwner() {
        return repoOwner;
    }

    /**
     * Gets the repo name.
     *
     * @return the repo name
     */
    public String getRepoName() {
        return repoName;
    }

    /**
     * Gets the branch name.
     *
     * @return the branch name
     */
    public String getBranchName() {
        return branchName;
    }

    /**
     * Gets the HTTP endpoint URL.
     *
     * @return the HTTP endpoint URL
     */
    public String getHttpEndpoint() {
        return httpEndpoint;
    }

    /**
     * Gets the HTTP credentials id.
     *
     * @return the credentials id
     */
    public String getHttpCredentialsId() {
        return httpCredentialsId;
    }

    /**
     * Gets whether to enable SSL verify.
     *
     * @return true if verify SSL is enabled
     */
    public boolean getHttpVerifySSL() {
        return httpVerifySSL;
    }

    /**
     * Returns credentials for accessing the HTTP endpoint if they are configured.
     *
     * @return credentials; null if not provided
     */
    @CheckForNull
    public UsernamePasswordCredentials getCredentials() {
        return !Strings.isNullOrEmpty(httpCredentialsId) ?
                BuildStatusConfig.getCredentials(UsernamePasswordCredentials.class,
                        httpCredentialsId) :
                null;
    }

    public static HttpNotifierConfig fromGlobalConfig(String repoOwner, String repoName, String branchName) {
        BuildStatusConfig config = BuildStatusConfig.get();
        HttpNotifierConfig httpNotifierConfig = null;

        if (config.getEnableHttp()) {
            httpNotifierConfig = new HttpNotifierConfig();
            httpNotifierConfig.repoOwner = repoOwner;
            httpNotifierConfig.repoName = repoName;
            httpNotifierConfig.branchName = branchName;
            httpNotifierConfig.httpEndpoint = config.getHttpEndpoint();
            httpNotifierConfig.httpCredentialsId = config.getHttpCredentialsId();
            httpNotifierConfig.httpVerifySSL = config.getHttpVerifySSL();
        }

        return httpNotifierConfig;
    }
}
