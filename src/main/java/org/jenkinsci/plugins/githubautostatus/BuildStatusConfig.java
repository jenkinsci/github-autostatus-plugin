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

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.githubautostatus.model.BuildStage;
import org.jenkinsci.plugins.githubautostatus.model.BuildState;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.model.Run.XSTREAM2;

/**
 * Provides configuration options for this plugin.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
@Extension
public class BuildStatusConfig extends GlobalConfiguration {

    private String credentialsId;
    private String influxDbUrl;
    private String influxDbDatabase;
    @Deprecated
    private transient String influxDbUser;
    @Deprecated
    private transient String influxDbPassword;
    private String influxDbRetentionPolicy;
    private boolean enableInfluxDb;
    private boolean disableGithub;
    private boolean enableStatsd;
    private String statsdHost;
    private String statsdPort;
    private String statsdBucket;
    private String statsdMaxSize;
    private boolean enableHttp;
    private String httpEndpoint;
    private String httpCredentialsId;
    private boolean httpVerifySSL;
    private transient Integer dbVersion;


    /**
     * Adds compatibility aliases to prevent "old data" warnings
     */
    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void AddCompatilityAliases() {
        XSTREAM2.addCompatibilityAlias("org.jenkinsci.plugins.githubautostatus.BuildStageModel", BuildStage.class);
        XSTREAM2.addCompatibilityAlias("org.jenkinsci.plugins.githubautostatus.notifiers.BuildState", BuildState.class);
    }

    /**
     * Convenience method to get the configuration object
     *
     * @return the configuration object
     */
    public static BuildStatusConfig get() {
        return GlobalConfiguration.all().get(BuildStatusConfig.class);
    }

    /**
     * Default constructor - loads the configuration
     */
    public BuildStatusConfig() {
        load();
    }

    /**
     * Gets human readable name
     *
     * @return human readable name
     */
    @Override
    public String getDisplayName() {
        return "Global configuration object for the autostatus plugin";
    }

    /**
     * Invoked when the global configuration page is submitted
     *
     * @param req Request that represents the form submission
     * @param json The JSON object that captures the configuration data
     * @return always returns true (allow config page to be closed)
     * @throws hudson.model.Descriptor.FormException exception if a form field is invalid
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        return true;
    }

    /**
     * Gets whether sending status to GitHub is enabled. For compatibility reasons
     * the flag is stored as the inverse (disableGithub) so that GitHub is
     * enabled by default for users who upgrade from a version prior to the
     * flag.
     *
     * @return true if writing to GitHub is enabled
     */
    public boolean getEnableGithub() {
        return !disableGithub;
    }

    /**
     * Sets whether sending status to GitHub is enabled.
     *
     * @param enableGithub true to enable sending status to GitHub
     */
    @DataBoundSetter
    public void setEnableGithub(boolean enableGithub) {
        this.disableGithub = !enableGithub;
        save();
    }

    /**
     * Get a flag indicating whether to enable HTTP publisher
     *
     * @return true if writing to HTTP is enabled
     */
    public boolean getEnableHttp() {
        return enableHttp;
    }

    /**
     * Set whether sending status to HTTP endpoint is enabled
     *
     * @param enableHttp true to enable sending status to HTTP endpoint
     */
    @DataBoundSetter
    public void setEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
        save();
    }

    /**
     * Gets whether SSL verify is enabled
     *
     * @return true if verify SSL is enabled
     */
    public boolean getHttpVerifySSL() {
        return httpVerifySSL;
    }

    /**
     * Set whether to enable SSL verify
     *
     * @param httpVerifySSL true to verify SSL
     */
    @DataBoundSetter
    public void setHttpVerifySSL(boolean httpVerifySSL) {
        this.httpVerifySSL = httpVerifySSL;
        save();
    }

    /**
     * Get the credentials Id
     *
     * @return the credentials id
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Sets the credentials id.
     *
     * @param credentialsId the credentials id
     */
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        save();
    }

    /**
     * Get HTTP credentials ID
     *
     * @return the HTTP credentials
     */
    public String getHttpCredentialsId() {
        return httpCredentialsId;
    }

    /**
     * Sets the HTTP credentials Id
     *
     * @param httpCredentialsId the credentials Id
     */
    @DataBoundSetter
    public void setHttpCredentialsId(String httpCredentialsId) {
        this.httpCredentialsId = httpCredentialsId;
        save();
    }

    /**
     * Get HTTP URL endpoint
     *
     * @return http endpoint URL
     */
    public String getHttpEndpoint() {
        return httpEndpoint;
    }

    /**
     * Set HTTP endpoint URL
     *
     * @param httpEndpoint the HTTP URL
     */
    @DataBoundSetter
    public void setHttpEndpoint(String httpEndpoint) {
        this.httpEndpoint = httpEndpoint;
        save();
    }

    /**
     * Get flag determining whether writing to influxdb is enabled
     * Gets whether writing to InfluxDB is enabled.
     *
     * @return true if writing to InfluxDB is enabled
     */
    public boolean getEnableInfluxDb() {
        return enableInfluxDb;
    }

    /**
     * Sets whether writing to InfluxDB is enabled.
     *
     * @param enableInfluxDb true to enable writing to InfluxDB
     */
    @DataBoundSetter
    public void setEnableInfluxDb(boolean enableInfluxDb) {
        this.enableInfluxDb = enableInfluxDb;
        save();
    }

    /**
     * Gets the InfluxDB URL.
     *
     * @return the value of influxDbUrl
     */
    public String getInfluxDbUrl() {
        return influxDbUrl;
    }

    /**
     * Sets the InfluxDB URL.
     *
     * @param influxDbUrl new value of influxDbUrl
     */
    @DataBoundSetter
    public void setInfluxDbUrl(String influxDbUrl) {
        this.influxDbUrl = influxDbUrl;
        save();
    }

    /**
     * Gets the InfluxDB database.
     *
     * @return the value of influxDbDatabase
     */
    public String getInfluxDbDatabase() {
        return influxDbDatabase;
    }

    /**
     * Sets the InfluxDB database.
     *
     * @param influxDbDatabase new value of influxDbDatabase
     */
    @DataBoundSetter
    public void setInfluxDbDatabase(String influxDbDatabase) {
        this.influxDbDatabase = influxDbDatabase;
        save();
    }

    /**
     * Gets the InfluxDB retention policy.
     *
     * @return the value of influxDbRetentionPolicy
     */
    public String getInfluxDbRetentionPolicy() {
        return influxDbRetentionPolicy;
    }

    /**
     * Sets the InfluxDB retention policy.
     *
     * @param influxDbRetentionPolicy new value of influxDbRetentionPolicy
     */
    @DataBoundSetter
    public void setInfluxDbRetentionPolicy(String influxDbRetentionPolicy) {
        this.influxDbRetentionPolicy = influxDbRetentionPolicy;
        save();
    }

    /**
     * Get the InfluxDB database version.
     *
     * @return the InfluxDB database version
     */
    public Integer getDbVersion() { return dbVersion; }

    /**
     * Set the value of dbVersion
     *
     * @param dbVersion new value of dbVersion
     */
    @DataBoundSetter
    public void setDbVersion(String dbVersion) {
        this.dbVersion = Integer.parseInt(dbVersion);
        save();
    }

    public ListBoxModel doFillDbVersionItems() {
        ListBoxModel items = new ListBoxModel();
        items.add("Version 1 (legacy)", "1");
        items.add("Version 2 (recommended for new installs)", "2");
        return items;
    }

    /**
     * Gets whether writing to StatsD is enabled.
     *
     * @return true if writing to StatsD is enabled
     */
    public boolean getEnableStatsd() {
        return enableStatsd;
    }

    /**
     * Sets whether writing to StatsD is enabled.
     *
     * @param enableStatsd true to enable writing to StatsD
     */
    @DataBoundSetter
    public void setEnableStatsd(boolean enableStatsd) {
        this.enableStatsd = enableStatsd;
        save();
    }

    /**
     * Gets the StatsD host.
     *
     * @return the value of statsdHost
     */
    public String getStatsdHost() {
        return statsdHost;
    }

    /**
     * Sets the StatsD host.
     *
     * @param statsdHost new value of statsdHost
     */
    @DataBoundSetter
    public void setStatsdHost(String statsdHost) {
        this.statsdHost = statsdHost;
        save();
    }

    /**
     * Gets the StatsD port.
     *
     * @return the value of statsdPort
     */
    public String getStatsdPort() {
        return statsdPort;
    }

    /**
     * Sets the StatsD port.
     *
     * @param statsdPort new value of statsdPort
     */
    @DataBoundSetter
    public void setStatsdPort(String statsdPort) {
        this.statsdPort = statsdPort;
        save();
    }

    /**
     * Gets the StatsD bucket.
     *
     * @return the value of statsdBucket
     */
    public String getStatsdBucket() {
        return statsdBucket;
    }

    /**
     * Sets the StatsD bucket.
     *
     * @param statsdBucket new value of statsdBucket
     */
    @DataBoundSetter
    public void setStatsdBucket(String statsdBucket) {
        this.statsdBucket = statsdBucket;
        save();
    }

    /**
     * Gets the StatsD maximum packet size.
     *
     * @return the value of statsdMaxSize
     */
    public String getStatsdMaxSize() {
        return statsdMaxSize;
    }

    /**
     * Sets the StatsD maximum packet size.
     *
     * @param statsdMaxSize new value of statsdMaxSize
     */
    @DataBoundSetter
    public void setStatsdMaxSize(String statsdMaxSize) {
        this.statsdMaxSize = statsdMaxSize;
        save();
    }

    /**
     * Fill the list box in the settings page with valid credentials
     *
     * @param credentialsId the current credentials Id
     * @return ListBoxModel containing credentials to show
     */
    public ListBoxModel doFillCredentialsIdItems(
            @QueryParameter String credentialsId) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return new StandardListBoxModel().includeCurrentValue(credentialsId);
        }
        return new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM,
                        Jenkins.getInstance(),
                        StandardCredentials.class,
                        Collections.<DomainRequirement>emptyList(),
                        CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)))
                .includeCurrentValue(credentialsId);
    }

    /**
     * Fill the list box in the settings page with valid credentials
     *
     * @param credentialsId the current credentials Id
     * @return ListBoxModel containing credentials to show
     */
    public ListBoxModel doFillHttpCredentialsIdItems(
            @QueryParameter String credentialsId) {
        return doFillCredentialsIdItems(credentialsId);
    }

    /**
     * Validates the credentialsId
     *
     * @param item context for validation
     * @param value to validate
     * @return FormValidation
     */
    public FormValidation doCheckCredentialsId(
            @AncestorInPath Item item,
            @QueryParameter String value) {
        if (item == null) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
        }
        if (StringUtils.isEmpty(value)) {
            return FormValidation.ok();
        }
        if (null == getCredentials(UsernamePasswordCredentials.class, value)) {
            return FormValidation.error("Cannot find currently selected credentials");
        }
        return FormValidation.ok();
    }

    /**
     * Validates the credentialsId
     *
     * @param item context for validation
     * @param value to validate
     * @return FormValidation
     */
    public FormValidation doCheckHttpCredentialsId(
            @AncestorInPath Item item,
            @QueryParameter String value) {
        return doCheckCredentialsId(item, value);
    }

    public FormValidation doCheckHttpEndpoint(@AncestorInPath Item item, @QueryParameter String value) {
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            return FormValidation.error("Invalid URL");
        }
        return FormValidation.ok();
    }

    public static <T extends Credentials> T getCredentials(@Nonnull Class<T> type, @Nonnull String credentialsId) {
        return CredentialsMatchers.firstOrNull(lookupCredentials(
                type, Jenkins.getInstance(), ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.allOf(
                CredentialsMatchers.withId(credentialsId),
                CredentialsMatchers.instanceOf(type)));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private Object readResolve() throws IOException {
        if (influxDbUser != null || influxDbPassword != null) {
            influxDbUser = null;
            influxDbPassword = null;
            save();
        }
        if (dbVersion == null) {
            dbVersion = 2;
            save();
        }
        return this;
    }
}
