package org.jenkinsci.plugins.githubautostatus.model;

import hudson.plugins.jacoco.JacocoBuildAction;
import hudson.plugins.jacoco.model.Coverage;
import hudson.plugins.jacoco.model.CoverageElement;
import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JacocoBuildAction.class)
public class CodeCoverageTest {
  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testFromJacoco_null(){
    assertNull(CodeCoverage.fromJacoco(null));
  }

  @Test
  public void testFromJacoco(){
    // 100% branch
    Coverage branch = new Coverage(0, 100);
    branch.setType(CoverageElement.Type.BRANCH);
    // 67% line
    Coverage line  = new Coverage(50, 100);
    line.setType(CoverageElement.Type.LINE);
    // 50% method
    Coverage method = new Coverage(1, 1);
    method.setType(CoverageElement.Type.METHOD);
    // 75% class
    Coverage clazz = new Coverage(5, 15);
    clazz.setType(CoverageElement.Type.CLASS);

    Map<Coverage, Boolean> jacocoRatios = new HashMap<>();
    jacocoRatios.put(branch, true);
    jacocoRatios.put(line, true);
    jacocoRatios.put(method, true);
    jacocoRatios.put(clazz, true);

    JacocoBuildAction action = PowerMockito.mock(JacocoBuildAction.class);
    when(action.getCoverageRatios()).thenReturn(jacocoRatios);

    CodeCoverage coverage = CodeCoverage.fromJacoco(action);
    assertEquals(0.0, coverage.getFiles(), 0.001);
    assertEquals(0.0, coverage.getPackages(), 0.001);
    assertEquals(100.0, coverage.getConditionals(), 0.001);
    assertEquals(66.667, coverage.getLines(), 0.001);
    assertEquals(50.000, coverage.getMethods(), 0.001);
    assertEquals(75.000, coverage.getClasses(), 0.001);

  }
}
