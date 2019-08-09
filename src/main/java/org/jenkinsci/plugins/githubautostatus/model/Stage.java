package org.jenkinsci.plugins.githubautostatus.model;

import org.jenkinsci.plugins.githubautostatus.BuildState;

public class Stage {
  private String name;
  private boolean passed;
  private BuildState result;
  private long time;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public BuildState getResult() {
    return result;
  }

  public void setResult(BuildState result) {
    this.result = result;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }
}
