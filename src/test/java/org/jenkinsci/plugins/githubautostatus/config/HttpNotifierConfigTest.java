package org.jenkinsci.plugins.githubautostatus.config;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Descriptor.FormException;
import org.jenkinsci.plugins.githubautostatus.BuildStatusConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HttpNotifierConfigTest {

  private BuildStatusConfig config;
  private String httpCredentialsId = "mock-credential-id";
  private String httpEndpoint = "https://mock.example.com:8443/api?token=1q2w3e";
  private String httpUsername = "user-a";
  private String httpPassword = "something-secret";
  private boolean verifySSL = true;
  private MockedStatic<BuildStatusConfig> buildStatusConfigStatic;

  @BeforeEach
  public void setUp() {
    buildStatusConfigStatic = mockStatic(BuildStatusConfig.class);
    config = Mockito.mock(BuildStatusConfig.class);
    buildStatusConfigStatic.when(BuildStatusConfig::get).thenReturn(config);

    when(config.getEnableHttp()).thenReturn(true);
    when(config.getHttpEndpoint()).thenReturn(httpEndpoint);
    when(config.getHttpCredentialsId()).thenReturn(httpCredentialsId);
    when(config.getHttpVerifySSL()).thenReturn(verifySSL);
  }

  @AfterEach
  public void tearDown() {
    if (buildStatusConfigStatic != null) {
      buildStatusConfigStatic.close();
    }
  }

  @Test
  public void testGetRepoOwner() {
    String repoOwner = "mockRepoOwner";
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig(repoOwner, "", "");
    assertEquals(repoOwner, instance.getRepoOwner());
  }

  @Test
  public void testGetRepoName() {
    String repoName = "mockRepoName";
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", repoName, "");
    assertEquals(repoName, instance.getRepoName());
  }

  @Test
  public void testGetBranchName() {
    String branchName = "mockBranch";
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", "", branchName);
    assertEquals(branchName, instance.getBranchName());
  }

  @Test
  public void testGetHttpEndpoint() {
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", "", "");
    assertEquals(instance.getHttpEndpoint(), httpEndpoint);
  }

  @Test
  public void testGetHttpCredentialsId() {
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", "", "");
    assertEquals(instance.getHttpCredentialsId(), httpCredentialsId);
  }

  @Test
  public void testGetHttpVerifySSL() {
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", "", "");
    assertEquals(instance.getHttpVerifySSL(), verifySSL);
  }

  @Test
  public void testGetCredentialsNotEmpty() throws FormException {
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", "", "");
    StandardUsernamePasswordCredentials credentials =
            new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, httpCredentialsId,
                    "description", httpUsername, httpPassword);
    buildStatusConfigStatic.when(() -> BuildStatusConfig.getCredentials(UsernamePasswordCredentials.class, httpCredentialsId)).thenReturn(credentials);
    assertEquals(instance.getCredentials(), credentials);
    assertEquals(httpCredentialsId, credentials.getId());
    assertEquals(httpUsername, credentials.getUsername());
    assertEquals(httpPassword, credentials.getPassword().getPlainText());
  }

  @Test
  public void testGetCredentialsEmpty() {
    HttpNotifierConfig instance = HttpNotifierConfig.fromGlobalConfig("", "", "");
    buildStatusConfigStatic.when(() -> BuildStatusConfig.getCredentials(UsernamePasswordCredentials.class, "not-exist-credential")).thenReturn(null);
    assertNull(instance.getCredentials());
  }
}
