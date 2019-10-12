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

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.githubautostatus.BuildStatusConfig;
import org.jenkinsci.plugins.githubautostatus.notifiers.InfluxDbNotifierSchemas;

/**
 * Encapsulates the logic of determining influxdb configuration for a build.
 *
 * @author Jeff Pearce (jxpearce@godaddy.com)
 */
public class InfluxDbNotifierConfig extends AbstractNotifierConfig {

    private String repoOwner;
    private String repoName;
    private String branchName;
    private String influxDbUrlString;
    private String influxDbDatabase;
    private String influxDbCredentialsId;
    private String influxDbRetentionPolicy;
<<<<<<< HEAD
    private Integer schemaVersion = null;
=======
    private Integer schemaVersion;
>>>>>>> dc059d12770d7366aa0f60ac9c495c9572d42c18

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
     * Gets influx db url.
     *
     * @return influx db url.
     */
    public String getInfluxDbUrlString() {
        return influxDbUrlString;
    }

    public Integer getDbVersion() {
        if (schemaVersion == null || schemaVersion <= 0 || schemaVersion > InfluxDbNotifierSchemas.getSchemaCount() ) {
            return InfluxDbNotifierSchemas.getSchemaCount();
        }
        return schemaVersion;
    }

    /**
     * Determines if influx db url is reachable.
     *
     * @return true if url is reachable; false otherwise.
     */
    public Boolean influxDbIsReachable() {
        try {
            URL url;
            try {
                url = new URL(influxDbUrlString);
            } catch (MalformedURLException ex) {
                Logger.getLogger(InfluxDbNotifierConfig.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.connect();
        } catch (IOException ex) {
            Logger.getLogger(InfluxDbNotifierConfig.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * Gets the influx db to write to.
     *
     * @return influx db.
     */
    public String getInfluxDbDatabase() {
        return influxDbDatabase;
    }

    /**
     * Returns credentials for calling influxdb if they are configured.
     *
     * @return credentials; null if not provided.
     */
    @CheckForNull
    public UsernamePasswordCredentials getCredentials() {
        return !StringUtils.isEmpty(influxDbCredentialsId) ?
                BuildStatusConfig.getCredentials(UsernamePasswordCredentials.class,
                        influxDbCredentialsId) :
                null;
    }

    /**
     * Gets the optional retention policy.
     *
     * @return retention policy.
     */
    public String getInfluxDbRetentionPolicy() {
        return influxDbRetentionPolicy;
    }

    public InfluxDbNotifierSchemas.SchemaInfo getSchema() {
        return InfluxDbNotifierSchemas.getSchema(getDbVersion() - 1);
    }

    /**
     * Creates an influxdb notification config based on the global settings.
     *
     * @param repoOwner  repo owner.
     * @param repoName   repo name.
     * @param branchName branch name.
     * @return config.
     */
    public static InfluxDbNotifierConfig fromGlobalConfig(String repoOwner, String repoName, String branchName) {
        BuildStatusConfig config = BuildStatusConfig.get();

        InfluxDbNotifierConfig influxDbNotifierConfig = new InfluxDbNotifierConfig();

        if (config.getEnableInfluxDb()) {
            influxDbNotifierConfig.repoOwner = repoOwner;
            influxDbNotifierConfig.repoName = repoName;
            influxDbNotifierConfig.branchName = branchName;

            influxDbNotifierConfig.influxDbUrlString = config.getInfluxDbUrl();
            influxDbNotifierConfig.influxDbDatabase = config.getInfluxDbDatabase();
            influxDbNotifierConfig.influxDbCredentialsId = config.getCredentialsId();
            influxDbNotifierConfig.influxDbRetentionPolicy = config.getInfluxDbRetentionPolicy();
            influxDbNotifierConfig.schemaVersion = config.getDbVersion();
        }

        return influxDbNotifierConfig;
    }
}
