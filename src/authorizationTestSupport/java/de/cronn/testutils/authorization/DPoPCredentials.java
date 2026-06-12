package de.cronn.testutils.authorization;

import java.net.URI;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Creates DPoP credentials that send {@code Authorization: DPoP <accessToken>} and a
 * per-request {@code DPoP} proof header (RFC 9449).
 *
 * <p>Because the DPoP proof must bind to the exact HTTP method and URI of each request,
 * the proof cannot be pre-computed. The supplied {@link DPoPProofFactory} is called once
 * per request and is responsible for generating a fresh, signed proof JWT.
 *
 * @param name         a human-readable label for this principal, used in the output matrix
 * @param accessToken  the DPoP-bound access token issued by the authorization server
 * @param proofFactory a factory that generates a signed DPoP proof JWT for each request
 */
public record DPoPCredentials(String name, String accessToken, DPoPProofFactory proofFactory) implements Credentials {

	public DPoPCredentials {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(accessToken, "accessToken must not be null");
		Objects.requireNonNull(proofFactory, "proofFactory must not be null");
	}

	@Override
	public void applyTo(HttpHeaders headers, HttpMethod method, URI uri) {
		headers.set("Authorization", "DPoP " + accessToken);
		headers.set("DPoP", proofFactory.createProof(method, uri, accessToken));
	}
}
