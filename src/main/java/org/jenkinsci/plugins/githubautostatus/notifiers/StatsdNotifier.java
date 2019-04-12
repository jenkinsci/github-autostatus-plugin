
package org.jenkinsci.plugins.githubautostatus.notifiers;

import org.jenkinsci.plugins.githubautostatus.StatsdWrapper;

public class StatsdNotifier implements BuildNotifier {
    private StatsdWrapper client;
    private String prefix;

    public StatsdNotifier() {
        // TODO: Use the config.
        client = new StatsdWrapper("xyz", "localhost", 8125);
    }

    public boolean isEnabled() {
        return client != null;
    }

    // reponame = name of the job.
    // repo-org = folder path (just prefix).

    // nodeName = stage
    void notifyBuildState(String jobName, String nodeName, BuildState buildState) {
        //service.jenkins-kubernetes.pipeline.<folder>.<subfolder>.<job name>.branch.<branch>.stage.<stage name>.status.<stage result> (Counter metric)
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(prefix);
        pathBuilder.append(".");
        pathBuilder.append(jobName)
        client.increment()
    }
}