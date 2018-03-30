package org.infinispan.tutorial.simple.spring;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.servlet.http.HttpServletRequest;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.spring.provider.SpringRemoteCacheManager;
import org.infinispan.spring.session.configuration.EnableInfinispanRemoteHttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This tutorial shows how to run Spring Session with Infinispan and Spring Boot.
 */
@SpringBootApplication
@EnableInfinispanRemoteHttpSession(cacheName = "default")
@EnableCaching
public class SpringApp {

   @Bean
   public org.infinispan.client.hotrod.configuration.Configuration customConfiguration() {
      class TestCallHandler implements CallbackHandler {
         final private String username;
         final private char[] password;
         final private String realm;

         public TestCallHandler(String username, String realm, char[] password) {
            this.username = username;
            this.password = password;
            this.realm = realm;
         }

         public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
               if (callback instanceof NameCallback) {
                  NameCallback nameCallback = (NameCallback) callback;
                  nameCallback.setName(username);
               } else if (callback instanceof PasswordCallback) {
                  PasswordCallback passwordCallback = (PasswordCallback) callback;
                  passwordCallback.setPassword(password);
               } else if (callback instanceof AuthorizeCallback) {
                  AuthorizeCallback authorizeCallback = (AuthorizeCallback) callback;
                  authorizeCallback.setAuthorized(authorizeCallback.getAuthenticationID().equals(
                        authorizeCallback.getAuthorizationID()));
               } else if (callback instanceof RealmCallback) {
                  RealmCallback realmCallback = (RealmCallback) callback;
                  realmCallback.setText(realm);
               } else {
                  throw new UnsupportedCallbackException(callback);
               }
            }
         }
      }

      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      return new ConfigurationBuilder()
            .addServer()
            .host("127.0.0.1")
            .security()
//            .ssl()
//            .enable()
//            .trustStoreFileName(tccl.getResource("truststore.jks").getPath())
//            .trustStorePassword("storePass".toCharArray())
            .authentication()
            .enable()
//            .saslMechanism("PLAIN")
            .saslMechanism("DIGEST-MD5")
            .saslQop(SaslQop.AUTH)
            .serverName("myhotrodserver")
            .callbackHandler(new TestCallHandler("user", "ApplicationRealm", "password".toCharArray()))
            .build();
   }

   @RestController
   static class SessionController {

      @Autowired
      SpringRemoteCacheManager cacheManager;

      @RequestMapping("/session")
      public Map<String, String> session(HttpServletRequest request) {
         Map<String, String> result = new HashMap<>();
         String sessionId = request.getSession(true).getId();
         result.put("created:", sessionId);
         // By default Infinispan integration for Spring Session will use 'sessions' cache.
         result.put("active:", cacheManager.getCache("default").getNativeCache().keySet().toString());
         return result;
      }
   }

   public static void main(String[] args) {
      SpringApplication.run(SpringApp.class, args);
   }
}
