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
package org.jenkinsci.plugins.githubautostatus;

/**
 * Class for StatsD configuration notifier.
 * @author Shane Gearon (shane.gearon@hootsuite.com)
 */
public class StatsdNotifierConfig {

    private String repoOwner;
    private String repoName;
    private String branchName;
    private String statsdHost;
    private String statsdPort;
    private String statsdBucket;
    private String statsdMaxSize;

    /**
     * Gets the repo owner.
     *
     * @return repo owner.
     */
    public String getRepoOwner() {
        return repoOwner;
    }

    /**
     * Gets the repo name.
     *
     * @return repo name.
     */
    public String getRepoName() {
        return repoName;
    }

    /**
     * Gets the branch name.
     *
     * @return branch name.
     */
    public String getBranchName() {
        return branchName;
    }

    /**
     * Gets statsd url.
     *
     * @return statsd url.
     */
    public String getStatsdHost() {
        return statsdHost;
    }

    /**
     * Gets statsd port.
     *
     * @return statsd port.
     */
    public String getStatsdPort() {
        return statsdPort;
    }

    /**
     * Gets statsd bucket.
     *
     * @return statsd bucket.
     */
    public String getStatsdBucket() {
        return statsdBucket;
    }

    /**
     * Gets statsd max packet size.
     *
     * @return statsd max packet size.
     */
    public String getStatsdMaxSize() {
        return statsdMaxSize;
    }

    /**
     * Creates an statsd notification config based on the global settings.
     *
     * @param repoOwner repo owner.
     * @param repoName repo name.
     * @param branchName branch name.
     * @return config.
     */
    public static StatsdNotifierConfig fromGlobalConfig(String repoOwner, String repoName, String branchName) {
        BuildStatusConfig config = BuildStatusConfig.get();

        StatsdNotifierConfig statsdNotifierConfig = new StatsdNotifierConfig();

        if (config.getEnableStatsd()) {
            statsdNotifierConfig.repoOwner = repoOwner;
            statsdNotifierConfig.repoName = repoName;
            statsdNotifierConfig.branchName = branchName;

            statsdNotifierConfig.statsdHost = config.getStatsdHost();
            statsdNotifierConfig.statsdPort = config.getStatsdPort();
            statsdNotifierConfig.statsdBucket = config.getStatsdBucket();
            statsdNotifierConfig.statsdMaxSize = config.getStatsdMaxSize();
        }

        return statsdNotifierConfig;
    }
}
