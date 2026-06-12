package de.cronn.testutils.authorization;

import java.util.Objects;

import org.springframework.http.HttpHeaders;

/**
 * Creates bearer-token credentials sent as {@code Authorization: Bearer <token>}.
 *
 * @param name  a human-readable label for this principal, used in the output matrix
 * @param token a bearer token (e.g. a JWT or opaque token) that is authorized for exactly this principal
 */
public record BearerTokenCredentials(String name, String token) implements Credentials {

	public BearerTokenCredentials {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(token, "token must not be null");
	}

	@Override
	public void applyTo(HttpHeaders headers) {
		headers.setBearerAuth(token);
	}
}
