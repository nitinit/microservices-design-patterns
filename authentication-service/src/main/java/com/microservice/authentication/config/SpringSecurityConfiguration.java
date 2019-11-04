package com.microservice.authentication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.web.util.CustomDefaultErrorAttributes;
import com.microservice.jwt.common.TokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Map;

/**
 * Spring Security Configuration
 */
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final SharedAuthenticationService sharedAuthenticationService;

    private final TokenProvider tokenProvider;

    private final ObjectMapper objectMapper;

    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers()
            .and()
            .requestMatchers()
            .antMatchers("/login", "/oauth/authorize")
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .successHandler(successHandler())
            .permitAll()
            .and()
            .logout()
            .deleteCookies("SESSIONID")
            .invalidateHttpSession(true)
            .and()
            .csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringAntMatchers("/oauth/**")

            .and()
            .requestMatchers()
            .antMatchers("/api/**")
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginProcessingUrl("/api/authenticate").permitAll()
            .successHandler(successHandler())
            .failureHandler(authenticationFailureHandler())
            .and()
            .logout()
            .logoutUrl("/api/logout").permitAll()
            .deleteCookies("SESSIONID")
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            .invalidateHttpSession(true)
            .and()
            .csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

/*
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(new OAuth2AuthenticationEntryPoint());
*/
/*
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(new Http403ForbiddenEntryPoint());
*/
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (request.getPathInfo().startsWith("/api/")) {
                Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, exception, true);
                response.setStatus(Integer.parseInt(errorAttributes.get("status").toString()));
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().append(objectMapper.writeValueAsString(errorAttributes));
            } else {
                new SimpleUrlAuthenticationFailureHandler().onAuthenticationFailure(request, response, exception);
            }
        };
    }

    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            if (request.getPathInfo().startsWith("/api/")) {
                String authorization = "Bearer " + tokenProvider.createToken(authentication, authentication.getName(), "true" .equals(request.getParameter("rememberMe")));
                JwtTokenDto jwtToken = new JwtTokenDto(authorization);
                response.addHeader(HttpHeaders.AUTHORIZATION, authorization);
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.addHeader("sessionId", request.getSession().getId());
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().append(objectMapper.writeValueAsString(jwtToken));
            } else {
                new SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

}
