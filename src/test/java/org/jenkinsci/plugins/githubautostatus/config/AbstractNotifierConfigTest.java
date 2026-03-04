package org.jenkinsci.plugins.githubautostatus.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractNotifierConfigTest {

  private AbstractNotifierConfig notifierConfig;

  @BeforeEach
  public void setUp() {
    notifierConfig = Mockito.mock(AbstractNotifierConfig.class, Mockito.CALLS_REAL_METHODS);
  }

  @Test
  public void testGetHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    CloseableHttpClient httpClient = notifierConfig.getHttpClient(false);
    assertNotNull(httpClient);
  }

  @Test
  public void testGetHttpClientIgnoreSSLVerify() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    CloseableHttpClient httpClient = notifierConfig.getHttpClient(true);
    assertNotNull(httpClient);
  }
}
