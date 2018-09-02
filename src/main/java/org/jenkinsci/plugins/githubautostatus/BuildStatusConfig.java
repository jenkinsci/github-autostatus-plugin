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
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author jxpearce
 */
/**
 * Provides configuration options for this plugin.
 *
 * @author jxpearce@godaddy.com
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
     * @throws hudson.model.Descriptor.FormException
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        return true;
    }

    /**
     * Get a flag indicating whether to enable GitHub. For compatibility reasons
     * the flag is stored as the inverse (disableGithub) so that GitHub is
     * enabled by default for users who upgrade from a version prior to the
     * flag.
     *
     * @return true if writing to github is enabled
     */
    public boolean getEnableGithub() {
        return !disableGithub;
    }

    /**
     * Set whether sending status to github is enabled
     *
     * @param enableGithub true to enable sending status to github
     */
    @DataBoundSetter
    public void setEnableGithub(boolean enableGithub) {
        this.disableGithub = !enableGithub;
        save();
    }

    /**
     * Get the credentials Id
     *
     * @return the credentials
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Sets the credentials Id
     *
     * @param credentialsId the credentials Id
     */
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        save();
    }

    /**
     * Get flag determining whether writing to influxdb is enabled
     *
     * @return true if writing to influxdb is enabled
     */
    public boolean getEnableInfluxDb() {
        return enableInfluxDb;
    }

    /**
     * Set whether writing to influxdb is enabled
     *
     * @param enableInfluxDb true to enable writing to influxdb
     */
    @DataBoundSetter
    public void setEnableInfluxDb(boolean enableInfluxDb) {
        this.enableInfluxDb = enableInfluxDb;
        save();
    }

    /**
     * Get the value of influxDbUrl
     *
     * @return the value of influxDbUrl
     */
    public String getInfluxDbUrl() {
        return influxDbUrl;
    }

    /**
     * Set the value of influxDbUrl
     *
     * @param influxDbUrl new value of influxDbUrl
     */
    @DataBoundSetter
    public void setInfluxDbUrl(String influxDbUrl) {
        this.influxDbUrl = influxDbUrl;
        save();
    }

    /**
     * Get the value of influxDbDatabase
     *
     * @return the value of influxDbDatabase
     */
    public String getInfluxDbDatabase() {
        return influxDbDatabase;
    }

    /**
     * Set the value of influxDbDatabase
     *
     * @param influxDbDatabase new value of influxDbDatabase
     */
    @DataBoundSetter
    public void setInfluxDbDatabase(String influxDbDatabase) {
        this.influxDbDatabase = influxDbDatabase;
        save();
    }

    /**
     * Get the value of influxDbRetentionPolicy
     *
     * @return the value of influxDbRetentionPolicy
     */
    public String getInfluxDbRetentionPolicy() {
        return influxDbRetentionPolicy;
    }

    /**
     * Set the value of influxDbRetentionPolicy
     *
     * @param influxDbRetentionPolicy new value of influxDbRetentionPolicy
     */
    @DataBoundSetter
    public void setInfluxDbRetentionPolicy(String influxDbRetentionPolicy) {
        this.influxDbRetentionPolicy = influxDbRetentionPolicy;
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
        return this;
    }
}
