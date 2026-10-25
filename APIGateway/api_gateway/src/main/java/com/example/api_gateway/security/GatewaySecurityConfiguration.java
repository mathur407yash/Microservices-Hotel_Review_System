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
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.net.URI;
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
			@Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin,
			@Value("${app.security.jwt.issuer}") String issuer,
			@Value("${app.security.jwt.audience}") String audience,
			@Value("${app.security.jwt.ttl-seconds:900}") long ttlSeconds) {
		return (request, response, authentication) -> {
			if (!(authentication.getPrincipal() instanceof OidcUser user)) {
				throw new IOException("Google sign-in did not return an OpenID Connect user");
			}

			String role = null;
			if (request.getSession(false) != null) {
				Object pendingRole = request.getSession(false)
						.getAttribute(RoleAwareAuthorizationRequestResolver.PENDING_ROLE_SESSION_ATTRIBUTE);
				if (pendingRole instanceof String selectedRole
						&& List.of("USER", "HOTEL_OWNER").contains(selectedRole)) {
					role = selectedRole;
				}
			}
			if (role == null) {
				throw new IOException("No valid account type was selected before Google sign-in");
			}

			Instant issuedAt = Instant.now();
			Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
			JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
					.issuer(issuer)
					.subject(user.getSubject())
					.audience(List.of(audience))
					.claim("roles", List.of(role))
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
			response.setContentType(MediaType.TEXT_HTML_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
			response.setHeader(HttpHeaders.PRAGMA, "no-cache");
			response.setHeader("Content-Security-Policy", "default-src 'none'; script-src 'unsafe-inline'; base-uri 'none'");
			String payload = serializeForInlineScript(objectMapper, Map.of(
					"type", "hotel-review-auth",
					"accessToken", token,
					"tokenType", "Bearer",
					"expiresIn", ttlSeconds,
					"role", role,
					"email", user.getEmail() == null ? "" : user.getEmail(),
					"name", user.getFullName() == null ? "" : user.getFullName()));
			String allowedOrigin = serializeForInlineScript(objectMapper, URI.create(frontendOrigin).toString());
			response.getWriter().write("""
					<!doctype html>
					<html lang="en">
					<head><meta charset="utf-8"><title>Sign-in complete</title></head>
					<body>
					<script>
					const payload = %s;
					const allowedOrigin = %s;
					if (window.opener) {
					  window.opener.postMessage(payload, allowedOrigin);
					  window.close();
					} else {
					  document.body.textContent = "Sign-in complete. You can close this window.";
					}
					</script>
					</body>
					</html>
					""".formatted(payload, allowedOrigin));
		};
	}

	private static String serializeForInlineScript(ObjectMapper objectMapper, Object value) throws IOException {
		return objectMapper.writeValueAsString(value)
				.replace("&", "\\u0026")
				.replace("<", "\\u003c")
				.replace(">", "\\u003e")
				.replace(String.valueOf((char) 0x2028), "\\u2028")
				.replace(String.valueOf((char) 0x2029), "\\u2029");
	}

	@Bean
	OAuth2AuthorizationRequestResolver authorizationRequestResolver(
			ClientRegistrationRepository clientRegistrationRepository) {
		return new RoleAwareAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(frontendOrigin));
		configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		configuration.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	@Order(1)
	SecurityFilterChain oauth2LoginFilterChain(
			HttpSecurity http,
			AuthenticationSuccessHandler googleLoginSuccessHandler,
			OAuth2AuthorizationRequestResolver authorizationRequestResolver) throws Exception {
		http.securityMatcher("/oauth2/**", "/login/**")
				.cors(cors -> {})
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorizationRequestResolver))
						.successHandler(googleLoginSuccessHandler))
				.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain apiFilterChain(
			HttpSecurity http,
			NimbusJwtDecoder jwtDecoder,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/users/**", "/adduser").hasRole("USER")
						.requestMatchers(HttpMethod.GET, "/hotels/owned").hasRole("HOTEL_OWNER")
						.requestMatchers(HttpMethod.POST, "/hotels/**").hasRole("HOTEL_OWNER")
						.requestMatchers(HttpMethod.DELETE, "/hotels/**").hasRole("HOTEL_OWNER")
						.requestMatchers(HttpMethod.GET, "/hotels/**").hasAnyRole("USER", "HOTEL_OWNER")
						.requestMatchers(HttpMethod.POST, "/ratings/**").hasRole("USER")
						.requestMatchers(HttpMethod.DELETE, "/ratings/**").hasRole("USER")
						.requestMatchers(HttpMethod.GET, "/ratings/user/**").hasRole("USER")
						.requestMatchers("/ratings/**").hasAnyRole("USER", "HOTEL_OWNER")
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

	private static OAuth2TokenValidator<Jwt> jwtValidator(String issuer, String audience) {
		OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error(
						"invalid_token", "Required JWT audience is missing", null));
		return new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer), audienceValidator);
	}
}
