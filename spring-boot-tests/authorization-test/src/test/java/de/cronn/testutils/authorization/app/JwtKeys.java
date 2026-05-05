package de.cronn.testutils.authorization.app;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public final class JwtKeys {

	private static final KeyPair KEY_PAIR = generate();

	private JwtKeys() {
	}

	public static RSAPublicKey publicKey() {
		return (RSAPublicKey) KEY_PAIR.getPublic();
	}

	public static RSAPrivateKey privateKey() {
		return (RSAPrivateKey) KEY_PAIR.getPrivate();
	}

	private static KeyPair generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
