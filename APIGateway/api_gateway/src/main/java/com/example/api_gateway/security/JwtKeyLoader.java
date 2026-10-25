package com.example.api_gateway.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class JwtKeyLoader {

	private JwtKeyLoader() {
	}

	static RSAPublicKey publicKey(String configuredKey) {
		try {
			byte[] keyBytes = decode(configuredKey, "PUBLIC KEY");
			return (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (GeneralSecurityException | ClassCastException exception) {
			throw new IllegalStateException("JWT_PUBLIC_KEY must contain an RSA X.509 public key", exception);
		}
	}

	static RSAPrivateKey privateKey(String configuredKey) {
		try {
			byte[] keyBytes = decode(configuredKey, "PRIVATE KEY");
			return (RSAPrivateKey) KeyFactory.getInstance("RSA")
					.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (GeneralSecurityException | ClassCastException exception) {
			throw new IllegalStateException("JWT_PRIVATE_KEY must contain an RSA PKCS#8 private key", exception);
		}
	}

	private static byte[] decode(String configuredKey, String pemLabel) {
		String key = configuredKey
				.replace("-----BEGIN " + pemLabel + "-----", "")
				.replace("-----END " + pemLabel + "-----", "")
				.replaceAll("\\s", "");
		try {
			return Base64.getDecoder().decode(key);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Configured JWT key is not valid Base64 or PEM", exception);
		}
	}
}
