package com.alumniplus.activity_service.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities =
                new ArrayList<>();

        Map<String, Object> realmAccess =
                jwt.getClaimAsMap("realm_access");

        if (realmAccess == null) {
            return authorities;
        }

        Object roles = realmAccess.get("roles");

        if (roles instanceof List<?> roleList) {

            for (Object role : roleList) {

                if (role instanceof String roleName) {

                    authorities.add(
                        new SimpleGrantedAuthority(
                            "ROLE_" +
                            roleName.toUpperCase(Locale.ROOT)
                        )
                    );
                }
            }
        }

        return authorities;
    }
}