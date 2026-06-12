package de.cronn.testutils.authorization;

import java.net.URI;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Creates HTTP Basic credentials sent as {@code Authorization: Basic <base64(username:password)>}.
 *
 * <p>The user account must be configured on the server with exactly the access described by
 * {@code name} — an account holding multiple roles produces misleading results in the matrix.
 *
 * @param name     a human-readable label for this principal, used in the output matrix
 * @param username the username
 * @param password the password
 */
public record BasicAuthCredentials(String name, String username, String password) implements Credentials {

	public BasicAuthCredentials {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(username, "username must not be null");
		Objects.requireNonNull(password, "password must not be null");
	}

	@Override
	public void applyTo(HttpHeaders headers, HttpMethod method, URI uri) {
		headers.setBasicAuth(username, password);
	}
}
