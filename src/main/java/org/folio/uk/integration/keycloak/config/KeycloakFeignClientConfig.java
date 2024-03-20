package org.folio.uk.integration.keycloak.config;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.ResourceUtils.getFile;

import feign.Client;
import feign.okhttp.OkHttpClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;

@Log4j2
public class KeycloakFeignClientConfig {

  @Bean
  public Client feignClient(KeycloakTlsProperties tlsProperties, okhttp3.OkHttpClient okHttpClient)
    throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
    var client = tlsProperties.isEnabled()
      ? sslClient(okHttpClient.newBuilder(), tlsProperties)
      : okHttpClient;

    return new OkHttpClient(client);
  }

  private static okhttp3.OkHttpClient sslClient(okhttp3.OkHttpClient.Builder clientBuilder,
    KeycloakTlsProperties properties)
    throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
    var keyStore = initKeyStore(properties);
    var trustManager = trustManager(keyStore);

    var sslSocketFactory = sslContext(trustManager).getSocketFactory();

    return clientBuilder.sslSocketFactory(sslSocketFactory, trustManager).build();
  }

  private static KeyStore initKeyStore(KeycloakTlsProperties properties)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    var trustStorePath = requireNonNull(properties.getTrustStorePath(), "Trust store path is not defined");
    var trustStorePassword = requireNonNull(properties.getTrustStorePassword(), "Trust store password is not defined");

    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (var is = new FileInputStream(getFile(trustStorePath))) {
      trustStore.load(is, trustStorePassword.toCharArray());
    }

    return trustStore;
  }

  private static X509TrustManager trustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
      TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers:"
        + Arrays.toString(trustManagers));
    }

    return (X509TrustManager) trustManagers[0];
  }

  private static SSLContext sslContext(X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {
    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager}, null);

    return sslContext;
  }
}
