package com.microservice.authentication.config;

import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import com.netflix.discovery.EurekaClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableAuthorizationServer
@AllArgsConstructor
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    private final AuthenticationManager authenticationManager;

    private final Java8SpringConfigurationProperties configurationProperties;

    private final PasswordEncoder passwordEncoder;

    private final EurekaClient eurekaClient;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.tokenStore(tokenStore())
            .authenticationManager(authenticationManager)
            .accessTokenConverter(jwtAccessTokenConverter());
    }

    private String[] getListOfServices() {
        return eurekaClient.getApplications()
            .getRegisteredApplications()
            .stream()
            .flatMap(a -> a.getInstances().stream())
            .map(a -> String.format("http://%s:%s/login", a.getIPAddr(), a.getPort()))
            .collect(Collectors.toList())
            .toArray(new String[] {});
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        String[] listOfServices = getListOfServices();
        log.debug("listOfServices: {}", listOfServices);
        clients.inMemory()
            .withClient("client")
            .secret(passwordEncoder.encode("secret"))
            .authorizedGrantTypes("password", "authorization_code", "refresh_token")
            .redirectUris(listOfServices)
            .autoApprove(true)
            .scopes("read", "write")
            .and()
            .withClient("actuator")
            .secret(passwordEncoder.encode("actuator_password"))
            .authorizedGrantTypes("client_credentials")
            .autoApprove(true)
            .authorities("ADMIN")
            .scopes("actuator");
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Bean
    JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        Java8SpringConfigurationProperties.Jwt jwt = configurationProperties.getJwt();
        if (StringUtils.isNotBlank(jwt.getBase64Secret())) {
            jwtAccessTokenConverter.setSigningKey(jwt.getBase64Secret());
            jwtAccessTokenConverter.setVerifierKey(jwt.getBase64Secret());
        } else {
            KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new FileSystemResource(jwt.getKeystore()), jwt.getKeystorePassword().toCharArray());
            jwtAccessTokenConverter.setKeyPair(keyStoreKeyFactory.getKeyPair(jwt.getKeystoreAlias()));
        }
        return jwtAccessTokenConverter;
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer
            .allowFormAuthenticationForClients()
            .tokenKeyAccess("permitAll()")
            .checkTokenAccess("isAuthenticated()");
    }
}
