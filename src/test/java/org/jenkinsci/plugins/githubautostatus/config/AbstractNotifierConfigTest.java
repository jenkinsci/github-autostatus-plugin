package org.jenkinsci.plugins.githubautostatus.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*"})
public class AbstractNotifierConfigTest {

  private AbstractNotifierConfig notifierConfig;

  @Before
  public void setUp() {
    notifierConfig = PowerMockito.mock(AbstractNotifierConfig.class, Mockito.CALLS_REAL_METHODS);
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
