package de.cronn.testutils.authorization.app;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.http.HttpMethod;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.cronn.testutils.authorization.DPoPProofFactory;

/**
 * A {@link DPoPProofFactory} for use in tests that generates signed DPoP proof JWTs using
 * a fresh RSA key pair. The corresponding JWK thumbprint can be retrieved via
 * {@link #jwkThumbprint()} for inclusion in the {@code cnf.jkt} claim of the access token.
 */
public class DPoPTestProofFactory implements DPoPProofFactory {

	private final RSAKey rsaKey;
	private final JWSSigner signer;

	public DPoPTestProofFactory() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair keyPair = generator.generateKeyPair();
			this.rsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
				.privateKey((java.security.interfaces.RSAPrivateKey) keyPair.getPrivate())
				.keyID(UUID.randomUUID().toString())
				.build();
			this.signer = new RSASSASigner(this.rsaKey);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to generate DPoP key pair", e);
		}
	}

	/**
	 * Returns the SHA-256 JWK thumbprint of the public key, for use as the {@code cnf.jkt}
	 * claim in the DPoP-bound access token.
	 */
	public String jwkThumbprint() {
		try {
			return rsaKey.toPublicJWK().computeThumbprint().toString();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to compute JWK thumbprint", e);
		}
	}

	@Override
	public String createProof(HttpMethod method, URI uri, String accessToken) {
		try {
			String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(
				MessageDigest.getInstance("SHA-256").digest(accessToken.getBytes(StandardCharsets.US_ASCII)));
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(new JOSEObjectType("dpop+jwt"))
				.jwk(rsaKey.toPublicJWK())
				.build();
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.jwtID(UUID.randomUUID().toString())
				.issueTime(new Date())
				.claim("htm", method.name())
				.claim("htu", uri.toString())
				.claim("ath", ath)
				.build();
			SignedJWT signedJWT = new SignedJWT(header, claims);
			signedJWT.sign(signer);
			return signedJWT.serialize();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create DPoP proof", e);
		}
	}
}
