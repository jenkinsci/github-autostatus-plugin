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
package org.jenkinsci.plugins.githubautostatus.notifiers;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jenkinsci.plugins.githubautostatus.InfluxDbNotifierConfig;
import org.jenkinsci.plugins.pipeline.modeldefinition.shaded.com.google.common.base.Strings;

/**
 *
 * @author jxpearce
 */
public class InfluxDbNotifier implements BuildNotifier {

    protected final String DEFAULT_STRING = "none";
    protected String repoOwner;
    protected String repoName;
    protected String branchName;
    protected String influxDbUrlString;
    protected InfluxDbNotifierConfig config;
    protected transient String authorization;

    /**
     * Constructor
     *
     * @param config influxdb configuration info
     */
    public InfluxDbNotifier(
            InfluxDbNotifierConfig config) {
        if (Strings.isNullOrEmpty(config.getInfluxDbUrlString()) || Strings.isNullOrEmpty(config.getInfluxDbDatabase())) {
            return;
        }
        String urlString = String.format("%s/write?db=%s", config.getInfluxDbUrlString(), config.getInfluxDbDatabase());
        try {
            UsernamePasswordCredentials credentials = config.getCredentials();
            if (credentials != null) {
                String influxDbUser = credentials.getUsername();
                String influxDbPassword = credentials.getPassword().getPlainText();
                
                authorization = Base64.getEncoder().encodeToString(String.format("%s:%s", influxDbUser, influxDbPassword).getBytes());
            }
            if (!Strings.isNullOrEmpty(config.getInfluxDbRetentionPolicy())) {
                urlString = urlString.concat(String.format("&rp=%s", URLEncoder.encode(config.getInfluxDbRetentionPolicy(), "UTF-8")));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(InfluxDbNotifier.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (config.influxDbIsReachable()) {
            this.repoOwner = config.getRepoOwner().replace(" ", "\\ ");
            this.repoName = config.getRepoName().replace(" ", "\\ ");
            this.branchName = config.getBranchName().replace(" ", "\\ ");
            if (Strings.isNullOrEmpty(this.repoOwner)) {
                this.repoOwner = DEFAULT_STRING;
            }
            if (Strings.isNullOrEmpty(this.repoName)) {
                this.repoName = DEFAULT_STRING;
            }
            if (Strings.isNullOrEmpty(this.branchName)) {
                this.branchName = DEFAULT_STRING;
            }
            this.influxDbUrlString = urlString;
            this.config = config;
        }
    }

    /**
     * Determine whether notifier is enabled
     *
     * @return true if enabled; false otherwise
     */
    @Override
    public boolean isEnabled() {
        return this.config != null;
    }

    /**
     * Send stage status notification to influx
     *
     * @param nodeName the node that has changed
     * @param buildState the new state
     */
    @Override
    public void notifyBuildState(String jobName, String nodeName, BuildState buildState) {
        notifyBuildStageStatus(jobName, nodeName, buildState, 0);
    }

    /**
     * Send a state change to influx
     *
     * @param jobName the name of the job
     * @param nodeName the node that has changed
     * @param buildState the new state
     * @param timingInfo timingInfo
     */
    @Override
    public void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long timingInfo) {

        if (buildState == BuildState.Pending) {
            return;
        }

        // Success and all Skipped stages are marked as successful
        int passed = buildState == BuildState.CompletedError ? 0 : 1;
        String transFormedNodeName = nodeName.replace(" ", "\\ ");
        String result = buildState.toString();

        String data = String.format("stage,jobname=%s,owner=%s,repo=%s,branch=%s,stagename=%s,result=%s stagetime=%d,passed=%d",
                jobName.replace(" ", "\\ "),
                repoOwner,
                repoName,
                branchName,
                transFormedNodeName,
                result,
                timingInfo,
                passed);
        postData(data);
    }

    /**
     * Send final build status to influx
     *
     * @param jobName the name of the job
     * @param buildState the new state
     * @param buildDuration overall build time
     */
    @Override
    public void notifyFinalBuildStatus(String jobName, BuildState buildState, long buildDuration) {
        int passed = buildState == BuildState.CompletedSuccess ? 1 : 0;

        String dataPoint = String.format("job,jobname=%s,owner=%s,repo=%s,branch=%s,result=%s jobtime=%d,passed=%d",
                jobName.replace(" ", "\\ "),
                repoOwner,
                repoName,
                branchName,
                buildState.toString(),
                buildDuration,
                passed);
        postData(dataPoint);
    }

    @Override
    public void sendNonStageError(String jobName, String nodeName) {
        notifyBuildState(jobName, nodeName, BuildState.CompletedError);
    }

    /**
     * Posts a new series data point
     *
     * @param seriesInfo the data point
     */
    private void postData(String seriesInfo) {
        try (CloseableHttpClient httpclient = config.getHttpClient()) {
            HttpPost httppost = new HttpPost(influxDbUrlString);

            httppost.setEntity(new StringEntity(seriesInfo));
            
            if (!Strings.isNullOrEmpty(authorization)) {
                httppost.setHeader("Authorization", String.format("Basic %s", authorization));
            }

            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode > 299) {
                    String statusLine = response.getStatusLine().toString();
                    log(Level.WARNING, "Could not write to influxdb - %s", statusLine);
                    log(Level.WARNING, "Influxdb url - %s", influxDbUrlString);
                    log(Level.WARNING, "Series - %s", seriesInfo);
                }
            }
        } catch (IOException ex) {
            log(Level.SEVERE, ex);
        }
    }

    private static void log(Level level, Throwable exception) {
        getLogger().log(level, null, exception);
    }

    private static void log(Level level, String format, Object... args) {
        getLogger().log(level, String.format(format, args));
    }

    private static Logger getLogger() {
        return Logger.getLogger(InfluxDbNotifier.class.getName());
    }
}
