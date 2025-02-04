package com.capp.spring_boot_pkcs11_example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.security.KeyStore;
import java.security.Security;
import java.util.List;
import java.util.function.Consumer;

@Configuration
@ConditionalOnExpression("${server.ssl.enabled} and '${server.ssl.key-store-type}' == 'PKCS11'")
public class PKCS11KeyStoreInitializer {

    @Value("${server.ssl.key-store-password}")
    private String pin;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslCustomizer() {
        return factory -> {
            try {
                var config = new ClassPathResource("pkcs11.cfg");

                var provider = Security.getProvider("SunPKCS11");
                provider = provider.configure(config.getFile().getAbsolutePath());
                Security.addProvider(provider);

                KeyStore keyStore = KeyStore.getInstance("PKCS11", provider);
                keyStore.load(null, pin.toCharArray());

                var sslStoreBundle = SslStoreBundle.of(keyStore, pin, null);
                var sslBundle = SslBundle.of(sslStoreBundle);

                factory.setSslBundles(new SslBundles() {
                    @Override
                    public SslBundle getBundle(String name) throws NoSuchSslBundleException {
                        return sslBundle;
                    }

                    @Override
                    public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException {
                    }

                    @Override
                    public List<String> getBundleNames() {
                        return List.of();
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure PKCS#11 SSL", e);
            }
        };
    }

}