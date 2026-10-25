package com.example.hotel_service.Security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class HotelSecurityConfiguration {

	@Bean
	SecurityFilterChain hotelSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/hotels/owned").hasRole("HOTEL_OWNER")
						.requestMatchers(HttpMethod.GET, "/hotels/**").hasAnyRole("USER", "HOTEL_OWNER")
						.requestMatchers(HttpMethod.POST, "/hotels/**").hasRole("HOTEL_OWNER")
						.requestMatchers(HttpMethod.DELETE, "/hotels/**").hasRole("HOTEL_OWNER")
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
						.decoder(jwtDecoder)
						.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}

	@Bean
	JwtDecoder hotelJwtDecoder(
			@Value("${app.security.jwt.public-key}") String publicKeyValue,
			@Value("${app.security.jwt.issuer}") String issuer,
			@Value("${app.security.jwt.audience}") String audience) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(readPublicKey(publicKeyValue)).build();
		OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error(
						"invalid_token", "Required JWT audience is missing", null));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
		return decoder;
	}

	private static RSAPublicKey readPublicKey(String configuredKey) {
		String key = configuredKey.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
		try {
			byte[] keyBytes = Base64.getDecoder().decode(key);
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (GeneralSecurityException | IllegalArgumentException | ClassCastException exception) {
			throw new IllegalStateException("JWT_PUBLIC_KEY must contain an RSA X.509 public key", exception);
		}
	}
}
