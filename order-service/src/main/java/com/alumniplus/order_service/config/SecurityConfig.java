package com.alumniplus.order_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;

@Configuration
@EnableMethodSecurity // Enables @PreAuthorize checks on controller and service methods.
public class SecurityConfig {

    @Bean
    // Configures how incoming HTTP requests are secured.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http 
                .csrf(csrf -> csrf.disable()) 
                .formLogin(form -> form.disable()) // Disables the HTML login form.
                .httpBasic(basic -> basic.disable()) // Disables HTTP Basic username/password login.
                .sessionManagement(session -> session 
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth 
                        .requestMatchers(HttpMethod.GET, "/api/orders/").permitAll() // Public root endpoint.
                        .requestMatchers(HttpMethod.POST, "/api/orders/test-db", "/api/orders/test-kafka").permitAll() // Public test endpoints.
                        .anyRequest().authenticated() // All other endpoints require a valid token.
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())) // Validates JWTs using the configured issuer.
                .build(); // Creates the configured security filter chain.
    }

}
