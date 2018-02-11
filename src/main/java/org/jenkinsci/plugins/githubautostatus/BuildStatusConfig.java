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

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
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
    
    private String influxDbUrl;

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
}
