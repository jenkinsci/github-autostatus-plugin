package org.jenkinsci.plugins.githubautostatus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nthienan
 */
public class BuildStatus {
    private String repoOwner;
    private String repoName;
    private String jobName;
    private String branch;
    private String buildUrl;
    private int buildNumber;
    private String trigger;
    private boolean blocked;
    private long blockedTime;
    private long duration;
    private boolean passed;
    private BuildStage.State result;
    private TestResults testResult;
    private CodeCoverage coverage;
    private List<BuildStage> stages;
    private long timestamp;

    public BuildStatus() {
        stages = new ArrayList<>();
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public long getBlockedTime() {
        return blockedTime;
    }

    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public BuildStage.State getResult() {
        return result;
    }

    public void setResult(BuildStage.State result) {
        this.result = result;
    }

    public TestResults getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResults testResult) {
        this.testResult = testResult;
    }

    public CodeCoverage getCoverage() {
        return coverage;
    }

    public void setCoverage(CodeCoverage coverage) {
        this.coverage = coverage;
    }

    public List<BuildStage> getStages() {
        return stages;
    }

    public void setStages(List<BuildStage> stages) {
        this.stages = stages;
    }

    public void addStage(BuildStage stage) {
        stages.add(stage);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
