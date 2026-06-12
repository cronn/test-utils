package de.cronn.testutils.authorization;

import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * Factory for generating DPoP proof JWTs (RFC 9449) for use in authorization testing.
 *
 * <p>A DPoP proof must be generated fresh for each request because it binds to the HTTP method
 * ({@code htm}), URI ({@code htu}), and access token hash ({@code ath}) of that specific
 * request, and carries a unique {@code jti} and current {@code iat} to prevent replay attacks.
 *
 * <p>Implementations are responsible for signing the proof with the client's private key and
 * embedding the corresponding public key in the JWT header as required by RFC 9449.
 *
 * <p>Example usage with a custom implementation:
 * <pre>{@code
 * DPoPProofFactory proofFactory = (method, uri, accessToken) ->
 *     myDPoPSigner.createProof(method.name(), uri.toString(), accessToken);
 * Credentials dpopCredentials = Credentials.dpop("ADMIN", accessToken, proofFactory);
 * }</pre>
 */
@FunctionalInterface
public interface DPoPProofFactory {

	/**
	 * Creates a signed DPoP proof JWT for the given request.
	 *
	 * @param method      the HTTP method of the request (used as the {@code htm} claim)
	 * @param uri         the full absolute URI of the request (used as the {@code htu} claim)
	 * @param accessToken the DPoP-bound access token (used to compute the {@code ath} claim)
	 * @return a signed DPoP proof JWT string
	 */
	String createProof(HttpMethod method, URI uri, String accessToken);
}
