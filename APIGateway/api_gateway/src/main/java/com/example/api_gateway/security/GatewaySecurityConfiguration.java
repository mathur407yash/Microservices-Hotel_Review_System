package com.example.api_gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class GatewaySecurityConfiguration {

	@Bean
	JwtEncoder jwtEncoder(
			@Value("${app.security.jwt.public-key}") String publicKeyValue,
			@Value("${app.security.jwt.private-key}") String privateKeyValue) {
		RSAKey rsaKey = new RSAKey.Builder(JwtKeyLoader.publicKey(publicKeyValue))
				.privateKey(JwtKeyLoader.privateKey(privateKeyValue))
				.keyID("hotel-review-api")
				.build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
	}

	@Bean
	NimbusJwtDecoder jwtDecoder(
			@Value("${app.security.jwt.public-key}") String publicKeyValue,
			@Value("${app.security.jwt.issuer}") String issuer,
			@Value("${app.security.jwt.audience}") String audience) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(JwtKeyLoader.publicKey(publicKeyValue)).build();
		decoder.setJwtValidator(jwtValidator(issuer, audience));
		return decoder;
	}

	@Bean
	AuthenticationSuccessHandler googleLoginSuccessHandler(
			JwtEncoder jwtEncoder,
			ObjectMapper objectMapper,
			@Value("${app.security.jwt.issuer}") String issuer,
			@Value("${app.security.jwt.audience}") String audience,
			@Value("${app.security.jwt.ttl-seconds:900}") long ttlSeconds) {
		return (request, response, authentication) -> {
			if (!(authentication.getPrincipal() instanceof OidcUser user)) {
				throw new IOException("Google sign-in did not return an OpenID Connect user");
			}

			Instant issuedAt = Instant.now();
			Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
			JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
					.issuer(issuer)
					.subject(user.getSubject())
					.audience(List.of(audience))
					.issuedAt(issuedAt)
					.expiresAt(expiresAt);
			if (user.getEmail() != null) {
				claims.claim("email", user.getEmail());
			}
			if (user.getFullName() != null) {
				claims.claim("name", user.getFullName());
			}

			String token = jwtEncoder.encode(JwtEncoderParameters.from(
					org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
					claims.build())).getTokenValue();

			response.setStatus(200);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
			response.setHeader(HttpHeaders.PRAGMA, "no-cache");
			objectMapper.writeValue(response.getWriter(), Map.of(
					"access_token", token,
					"token_type", "Bearer",
					"expires_in", ttlSeconds));
		};
	}

	@Bean
	@Order(1)
	SecurityFilterChain oauth2LoginFilterChain(
			HttpSecurity http,
			AuthenticationSuccessHandler googleLoginSuccessHandler) throws Exception {
		http.securityMatcher("/oauth2/**", "/login/**")
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2.successHandler(googleLoginSuccessHandler))
				.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain apiFilterChain(HttpSecurity http, NimbusJwtDecoder jwtDecoder) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(jwtDecoder)));
		return http.build();
	}

	private static OAuth2TokenValidator<Jwt> jwtValidator(String issuer, String audience) {
		OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error(
						"invalid_token", "Required JWT audience is missing", null));
		return new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer), audienceValidator);
	}
}
