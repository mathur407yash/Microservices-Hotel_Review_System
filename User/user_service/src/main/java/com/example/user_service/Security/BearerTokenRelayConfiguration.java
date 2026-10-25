package com.example.user_service.Security;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class BearerTokenRelayConfiguration {

	@Bean
	RequestInterceptor bearerTokenRelayInterceptor() {
		return requestTemplate -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
				requestTemplate.header("Authorization", "Bearer " + jwtAuthentication.getToken().getTokenValue());
			}
		};
	}
}
