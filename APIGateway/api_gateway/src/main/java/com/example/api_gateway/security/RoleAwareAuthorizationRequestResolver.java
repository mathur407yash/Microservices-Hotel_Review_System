package com.example.api_gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Set;

final class RoleAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

	static final String PENDING_ROLE_SESSION_ATTRIBUTE = RoleAwareAuthorizationRequestResolver.class.getName() + ".role";
	private static final Set<String> ALLOWED_ROLES = Set.of("USER", "HOTEL_OWNER");

	private final OAuth2AuthorizationRequestResolver delegate;

	RoleAwareAuthorizationRequestResolver(ClientRegistrationRepository registrations, String authorizationRequestBaseUri) {
		this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, authorizationRequestBaseUri);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		return rememberSelectedRole(request, delegate.resolve(request));
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		return rememberSelectedRole(request, delegate.resolve(request, clientRegistrationId));
	}

	private OAuth2AuthorizationRequest rememberSelectedRole(
			HttpServletRequest request,
			OAuth2AuthorizationRequest authorizationRequest) {
		if (authorizationRequest == null) {
			return null;
		}

		String role = request.getParameter("role");
		if (!ALLOWED_ROLES.contains(role)) {
			throw new IllegalArgumentException("Choose either USER or HOTEL_OWNER before starting Google sign-in");
		}

		HttpSession session = request.getSession(true);
		session.setAttribute(PENDING_ROLE_SESSION_ATTRIBUTE, role);
		return authorizationRequest;
	}
}
