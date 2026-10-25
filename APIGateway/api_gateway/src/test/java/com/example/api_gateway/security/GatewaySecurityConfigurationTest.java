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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
				encoder, objectMapper, "http://localhost:5173", "https://hotel-review-system", "hotel-review-api", 900);

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
						"name", "</script><script>alert(1)</script>"));
		DefaultOidcUser oidcUser = new DefaultOidcUser(
				List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
		OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
				oidcUser, oidcUser.getAuthorities(), "google");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession(true).setAttribute(RoleAwareAuthorizationRequestResolver.PENDING_ROLE_SESSION_ATTRIBUTE, "USER");
		MockHttpServletResponse response = new MockHttpServletResponse();

		successHandler.onAuthenticationSuccess(request, response, authentication);

		Matcher payloadMatcher = Pattern.compile("const payload = (\\{[^\\n]+});").matcher(response.getContentAsString());
		assertTrue(payloadMatcher.find());
		JsonNode body = objectMapper.readTree(payloadMatcher.group(1));
		assertFalse(response.getContentAsString().contains("</script><script>alert(1)</script>"));
		assertEquals(200, response.getStatus());
		assertEquals("Bearer", body.get("tokenType").asText());
		assertEquals("USER", body.get("role").asText());
		assertEquals(900, body.get("expiresIn").asInt());
		assertEquals("no-store", response.getHeader("Cache-Control"));

		Jwt decoded = decoder.decode(body.get("accessToken").asText());
		assertEquals("https://hotel-review-system", decoded.getIssuer().toString());
		assertEquals("hotel-review-api", decoded.getAudience().get(0));
		assertEquals("google-user-123", decoded.getSubject());
		assertEquals(List.of("USER"), decoded.getClaimAsStringList("roles"));
		assertEquals("user@example.com", decoded.getClaimAsString("email"));
		assertEquals("</script><script>alert(1)</script>", decoded.getClaimAsString("name"));
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
