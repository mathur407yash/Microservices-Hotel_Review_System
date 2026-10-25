package com.example.api_gateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewaySecurityConfigurationTest {

	@Test
	void googleLoginReturnsSignedJwtForConfiguredAudience() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
		String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

		GatewaySecurityConfiguration configuration = new GatewaySecurityConfiguration();
		var encoder = configuration.jwtEncoder(publicKey, privateKey);
		JwtDecoder decoder = configuration.jwtDecoder(publicKey, "https://hotel-review-system", "hotel-review-api");
		ObjectMapper objectMapper = new ObjectMapper();
		AuthenticationSuccessHandler successHandler = configuration.googleLoginSuccessHandler(
				encoder, objectMapper, "https://hotel-review-system", "hotel-review-api", 900);

		Instant now = Instant.now();
		OidcIdToken idToken = new OidcIdToken(
				"google-id-token",
				now,
				now.plusSeconds(3600),
				Map.of(
						"iss", "https://accounts.google.com",
						"sub", "google-user-123",
						"aud", List.of("google-client-id"),
						"iat", now,
						"exp", now.plusSeconds(3600),
						"email", "user@example.com",
						"name", "Example User"));
		DefaultOidcUser oidcUser = new DefaultOidcUser(
				List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
		OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
				oidcUser, oidcUser.getAuthorities(), "google");
		MockHttpServletResponse response = new MockHttpServletResponse();

		successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

		JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
		assertEquals(200, response.getStatus());
		assertEquals("Bearer", body.get("token_type").asText());
		assertEquals(900, body.get("expires_in").asInt());
		assertEquals("no-store", response.getHeader("Cache-Control"));

		Jwt decoded = decoder.decode(body.get("access_token").asText());
		assertEquals("https://hotel-review-system", decoded.getIssuer().toString());
		assertEquals("hotel-review-api", decoded.getAudience().get(0));
		assertEquals("google-user-123", decoded.getSubject());
		assertEquals("user@example.com", decoded.getClaimAsString("email"));
		assertNotNull(decoded.getExpiresAt());
		assertEquals(900, decoded.getExpiresAt().getEpochSecond() - decoded.getIssuedAt().getEpochSecond());

		String wrongAudienceToken = encoder.encode(JwtEncoderParameters.from(
				org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
				JwtClaimsSet.builder()
						.issuer("https://hotel-review-system")
						.subject("google-user-123")
						.audience(List.of("another-api"))
						.issuedAt(now)
						.expiresAt(now.plusSeconds(60))
						.build())).getTokenValue();
		assertThrows(JwtException.class, () -> decoder.decode(wrongAudienceToken));
	}
}
