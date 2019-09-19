package org.jenkinsci.plugins.githubautostatus.model;

import hudson.model.Result;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildStateTest {

  @Test
  public void testFromResultSuccess(){
    assertEquals(BuildState.CompletedSuccess, BuildState.fromResult(Result.SUCCESS));
  }

  @Test
  public void testFromResultUnstable(){
    assertEquals(BuildState.Unstable, BuildState.fromResult(Result.UNSTABLE));
  }

  @Test
  public void testFromResultFailure(){
    assertEquals(BuildState.CompletedError, BuildState.fromResult(Result.FAILURE));
  }

  @Test
  public void testFromResultNotBuilt(){
    assertEquals(BuildState.NotBuild, BuildState.fromResult(Result.NOT_BUILT));
  }

  @Test
  public void testFromResultAborted(){
    assertEquals(BuildState.Aborted, BuildState.fromResult(Result.ABORTED));
  }
}
