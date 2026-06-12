package de.cronn.testutils.authorization;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Named credentials applied to an HTTP request during authorization testing.
 *
 * <p>Each instance represents one principal to probe. The {@code name} is used in the output
 * matrix and must be unique across all credentials passed to {@link AuthorizationTestUtil}.
 * Bearer-token and HTTP Basic credentials may be freely mixed in a single test case — both
 * authentication schemes ultimately resolve to Spring {@code GrantedAuthority} on the server side.
 *
 * <p>The credentials must grant <em>only</em> the access described by {@code name} — bundling
 * multiple roles in one token or user account produces misleading results in the matrix.
 *
 * <p>The following implementations are provided:
 * <ul>
 *   <li>{@link BearerTokenCredentials} — {@code Authorization: Bearer <token>}
 *   <li>{@link BasicAuthCredentials} — {@code Authorization: Basic <base64(username:password)>}
 *   <li>{@link DPoPCredentials} — {@code Authorization: DPoP <token>} + per-request {@code DPoP} proof header
 * </ul>
 * <p>
 * Create your own implementations for other auth schemes.
 */
public interface Credentials {

	/**
	 * A human-readable label for this principal, used in the output matrix
	 * (e.g. {@code "ADMIN"}, {@code "alice"}).
	 */
	String name();

	/**
	 * Apply these credentials to the given request headers.
	 *
	 * @param headers the request headers to modify
	 * @param method  the HTTP method of the request (needed by DPoP proof generation)
	 * @param uri     the full absolute URI of the request (needed by DPoP proof generation)
	 */
	void applyTo(HttpHeaders headers, HttpMethod method, URI uri);
}
