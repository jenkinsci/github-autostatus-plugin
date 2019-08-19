package org.jenkinsci.plugins.githubautostatus.model;

import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierConstants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BuildStageTest {

  @Test
  public void testStatePending() {
    BuildStage instance = new BuildStage("stage 1", new HashMap<>(), BuildStage.State.Pending);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testStateSkippedConditional() {
    BuildStage instance = new BuildStage("stage 1", new HashMap<>(), BuildStage.State.SkippedConditional);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testStateSkippedUnstable() {
    BuildStage instance = new BuildStage("stage 1", new HashMap<>(), BuildStage.State.SkippedUnstable);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testStateSkippedFailure() {
    BuildStage instance = new BuildStage("stage 1", new HashMap<>(), BuildStage.State.SkippedFailure);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testStateCompletedSuccess() {
    BuildStage instance = new BuildStage("stage 1", new HashMap<>(), BuildStage.State.CompletedSuccess);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testStateCompletedError() {
    BuildStage instance = new BuildStage("stage 1", new HashMap<>(), BuildStage.State.CompletedError);
    assertFalse(instance.isPassed());
  }

  @Test
  public void testGetStageName(){
    String stageName = "stage-2";
    BuildStage instance = new BuildStage(stageName);
    assertEquals(stageName, instance.getStageName());
  }

  @Test
  public void testAddToEnvironment() {
    Map<String, Object> environments = new HashMap<>();
    environments.put(BuildNotifierConstants.STAGE_DURATION, 1234L);
    BuildStage instance = new BuildStage("stage-2", environments);
    instance.addToEnvironment("key", "value");
    assertEquals(instance.getDuration(), 1234);
  }

  @Test
  public void testAddToEnvironmentOverrideDuration() {
    Map<String, Object> environments = new HashMap<>();
    BuildStage instance = new BuildStage("stage-2", environments);
    assertEquals(instance.getDuration(), 0);
    instance.addToEnvironment(BuildNotifierConstants.STAGE_DURATION, 1234L);
    assertEquals(instance.getDuration(), 1234);
  }

  @Test
  public void testAddAllToEnvironment() {
    BuildStage instance = new BuildStage("stage-2");
    instance.addToEnvironment("key", "value");
    Map<String, Object> environments = new HashMap<>();
    environments.put("key", "value");
    instance.addAllToEnvironment(environments);
    assertEquals(instance.getDuration(), 0);
  }

  @Test
  public void testAddAllToEnvironmentOverrideDuration() {
    BuildStage instance = new BuildStage("stage-2");
    instance.addToEnvironment("key", "value");
    Map<String, Object> environments = new HashMap<>();
    environments.put(BuildNotifierConstants.STAGE_DURATION, 1234L);
    assertEquals(instance.getDuration(), 0);
    instance.addAllToEnvironment(environments);
    assertEquals(instance.getDuration(), 1234L);
  }

  @Test
  public void testSetStatePending() {
    BuildStage instance = new BuildStage("stage-2");
    instance.setBuildState(BuildStage.State.Pending);
    assertEquals(instance.getBuildState(), BuildStage.State.Pending);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testSetStateSkippedConditional() {
    BuildStage instance = new BuildStage("stage-2");
    instance.setBuildState(BuildStage.State.SkippedConditional);
    assertEquals(instance.getBuildState(), BuildStage.State.SkippedConditional);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testSetStateSkippedUnstable() {
    BuildStage instance = new BuildStage("stage-2");
    instance.setBuildState(BuildStage.State.SkippedUnstable);
    assertEquals(instance.getBuildState(), BuildStage.State.SkippedUnstable);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testSetStateSkippedFailure() {
    BuildStage instance = new BuildStage("stage-2");
    instance.setBuildState(BuildStage.State.SkippedFailure);
    assertEquals(instance.getBuildState(), BuildStage.State.SkippedFailure);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testSetStateCompletedSuccess() {
    BuildStage instance = new BuildStage("stage-2");
    instance.setBuildState(BuildStage.State.CompletedSuccess);
    assertEquals(instance.getBuildState(), BuildStage.State.CompletedSuccess);
    assertTrue(instance.isPassed());
  }

  @Test
  public void testSetStateCompletedError() {
    BuildStage instance = new BuildStage("stage-2");
    instance.setBuildState(BuildStage.State.CompletedError);
    assertEquals(instance.getBuildState(), BuildStage.State.CompletedError);
    assertFalse(instance.isPassed());
  }
}
