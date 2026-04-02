package org.jenkinsci.plugins.githubautostatus;

import org.datadog.jenkins.plugins.datadog.clients.ClientHolder;
import org.datadog.jenkins.plugins.datadog.metrics.Metrics;
import org.datadog.jenkins.plugins.datadog.metrics.MetricsClient;

/**
 * StatsD wrapper that uses the Datadog plugin's client.
 */
public class DatadogPluginStatsdWrapper implements StatsdWrapper {

    private final String prefix;

    public DatadogPluginStatsdWrapper(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void increment(String key, int amount) {
        Metrics m = Metrics.getInstance();
        if (m != null) {
            while (amount > 0) {
                m.incrementCounter(prefix + "." + key, null, null);
                amount = amount - 1;
            }
        }
    }

    @Override
    public void time(String key, long duration) {
        MetricsClient mc = ClientHolder.getClient().metrics();
        if (mc != null) mc.gauge(prefix + "." + key, duration, null, null);
    }
}
