
package org.jenkinsci.plugins.githubautostatus.notifiers;

import org.jenkinsci.plugins.githubautostatus.StatsdWrapper;
import org.jenkinsci.plugins.githubautostatus.StatsdNotifierConfig;

public class StatsdNotifier {
    private StatsdWrapper client;
    protected StatsdNotifierConfig config;

    public StatsdNotifier(StatsdNotifierConfig cfg) {
        config = cfg;
        client = new StatsdWrapper(cfg.getStatsdBucket(), cfg.getStatsdHost(), Integer.parseInt(cfg.getStatsdPort()));
    }

    public boolean isEnabled() {
        return client != null;
    }


    private String getBranchPath() {
        return String.format("pipeline.%s.%s.branch.%s", config.getRepoOwner(), config.getRepoName(), config.getBranchName());
    }
    //service.jenkins-kubernetes.pipeline.<folder>.<subfolder>.<job name>.branch.<branch>.stage.<stage name>.duration (Timer metric)
    public void notifyBuildState(String jobName, String nodeName, BuildState buildState) {
        String fqp = String.format("%s.stage.%s.status.%s", getBranchPath(), nodeName, buildState);  
        client.increment(fqp, 1);
    }

    public void notifyBuildStageStatus(String jobName, String nodeName, BuildState buildState, long nodeDuration) {
        String fqp = String.format("%s.stage.%s.duration", getBranchPath(), nodeName);
        client.time(fqp, nodeDuration);
    }

    // 1 & 2.
    public void notifyFinalBuildStatus(String jobName, BuildState buildState, long buildDuration, long blockedDuration) {
        String fqp = String.format("%s.job.status.%s", getBranchPath(), buildState);
        client.increment(fqp, 1);
        fqp = String.format("%s.job.duration", getBranchPath());
        client.time(fqp, buildDuration);
        fqp = String.format("%s.job.blocked_duration", getBranchPath());
        client.time(fqp, buildDuration);
    }
}