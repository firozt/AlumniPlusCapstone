package com.alumniplus.order_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build(); // Creates the configured security filter chain.
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        var scopes = new JwtGrantedAuthoritiesConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new ArrayList<>(scopes.convert(jwt));
            // Keycloak realm roles become Spring roles, e.g. TRADERS -> ROLE_TRADERS.
            if (jwt.getClaim("realm_access") instanceof Map<?, ?> realm
                    && realm.get("roles") instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String name) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
                    }
                }
            }
            return authorities;
        });
        return converter;
    }

}
