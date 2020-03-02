package org.jenkinsci.plugins.githubautostatus.model;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.cobertura.CoberturaBuildAction;
import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.jacoco.JacocoBuildAction;
import hudson.plugins.jacoco.model.Coverage;
import hudson.plugins.jacoco.model.CoverageElement;
import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JacocoBuildAction.class, CoberturaBuildAction.class})
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
  public void testFromCoberturaNull(){
      assertNull(CodeCoverage.fromCobertura(null));
  }
  
  @Test
  public void testFromCobertura(){
    CoberturaBuildAction action = PowerMockito.mock(CoberturaBuildAction.class);
    AbstractBuild build = mock(AbstractBuild.class);
    when(action.getOwner()).thenReturn(build);
    File file = mock(File.class);
    when(build.getRootDir()).thenReturn(file);
    Map<CoverageMetric, Ratio> results = new HashMap<>();
    results.put(CoverageMetric.CONDITIONAL, Ratio.create(1, 10));
    results.put(CoverageMetric.CLASSES, Ratio.create(2, 10));
    results.put(CoverageMetric.FILES, Ratio.create(3, 10));
    results.put(CoverageMetric.LINE, Ratio.create(4, 10));
    results.put(CoverageMetric.METHOD, Ratio.create(5, 10));
    results.put(CoverageMetric.PACKAGES, Ratio.create(6, 10));
    
    when(action.getResults()).thenReturn(results);
    
    CodeCoverage coverage = CodeCoverage.fromCobertura(action);
    assertNotNull(coverage);
    assertEquals(10, coverage.getConditionals(), 0);
    assertEquals(20, coverage.getClasses(), 0);
    assertEquals(30, coverage.getFiles(), 0);
    assertEquals(40, coverage.getLines(), 0);
    assertEquals(50, coverage.getMethods(), 0);
    assertEquals(60, coverage.getPackages(), 0);
    assertEquals(-1f, coverage.getInstructions(), 0);

  }

  @Test
  public void testFromJacocoNull(){
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
    // 70.5% instruction
    Coverage instruction = new Coverage(50, 120);
    instruction.setType(CoverageElement.Type.INSTRUCTION);

    Map<Coverage, Boolean> jacocoRatios = new HashMap<>();
    jacocoRatios.put(branch, true);
    jacocoRatios.put(line, true);
    jacocoRatios.put(method, true);
    jacocoRatios.put(clazz, true);
    jacocoRatios.put(instruction, true);

    JacocoBuildAction action = PowerMockito.mock(JacocoBuildAction.class);
    when(action.getCoverageRatios()).thenReturn(jacocoRatios);

    CodeCoverage coverage = CodeCoverage.fromJacoco(action);
    assertEquals(-1f, coverage.getFiles(), 0.001);
    assertEquals(-1f, coverage.getPackages(), 0.001);
    assertEquals(100.0, coverage.getConditionals(), 0.001);
    assertEquals(66.667, coverage.getLines(), 0.001);
    assertEquals(50.000, coverage.getMethods(), 0.001);
    assertEquals(75.000, coverage.getClasses(), 0.001);
    assertEquals(70.588, coverage.getInstructions(), 0.001);
  }
}
