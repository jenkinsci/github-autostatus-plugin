package org.jenkinsci.plugins.githubautostatus;

import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

/**
 * StatsD wrapper that uses the Datadog plugin's client.
 */
public class DatadogPluginStatsdWrapper implements StatsdWrapper {

    private String prefix;

    public DatadogPluginStatsdWrapper(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void increment(String key, int amount) {
        DatadogUtilities.getDatadogClient().increment(prefix + "." + key, amount, null, null);
    }

    @Override
    public void time(String key, long duration) {
        DatadogUtilities.getDatadogClient().gauge(prefix + "." + key, duration, null, null);
    }
}
