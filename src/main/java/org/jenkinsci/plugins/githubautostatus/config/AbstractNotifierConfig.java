package org.jenkinsci.plugins.githubautostatus.config;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public abstract class AbstractNotifierConfig {

    /**
     * Gets an HTTP client that can be used to make requests.
     *
     * @return HTTP client
     */
    public CloseableHttpClient getHttpClient(boolean ignoreSSL) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (ignoreSSL) {
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                    .build();
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                            .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                            .build()
            );
            return HttpClientBuilder.create()
                    .setSSLContext(sslContext)
                    .setConnectionManager(connectionManager)
                    .build();
        }
        return HttpClients.createDefault();
    }
}
