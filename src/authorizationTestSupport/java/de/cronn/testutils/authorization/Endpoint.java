package de.cronn.testutils.authorization;

import org.springframework.http.HttpMethod;

/**
 * An HTTP endpoint discovered from the application's request mappings.
 *
 * @param method the HTTP method
 * @param path   the path pattern as registered in the request mapping (may contain path variables)
 */
public record Endpoint(HttpMethod method, String path) {
}
