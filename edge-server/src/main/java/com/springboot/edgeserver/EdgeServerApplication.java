package com.springboot.edgeserver;

import com.microservice.authentication.resourceserver.config.ActuatorResourceServerConfiguration;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.net.ssl.HttpsURLConnection;

@SpringBootApplication
@EnableZuulProxy
@EnableDiscoveryClient
@Import(ActuatorResourceServerConfiguration.class)
@EnableRedisHttpSession
public class EdgeServerApplication {
	static {
		HttpsURLConnection.setDefaultHostnameVerifier(new NoopHostnameVerifier());
	}

	public static void main(String[] args) {
		SpringApplication.run(EdgeServerApplication.class, args);
	}

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSIONID");
		serializer.setCookiePath("/");
		return serializer;
	}

	@Bean
	CorsFilter corsWebFilter() {
		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.applyPermitDefaultValues();
		corsConfig.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);

		return new CorsFilter(source);
	}

}

