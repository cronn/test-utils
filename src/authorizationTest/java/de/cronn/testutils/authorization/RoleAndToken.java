package de.cronn.testutils.authorization;

import java.util.Objects;

/**
 * Associates a role name with a Bearer access token to be used during authorization testing.
 *
 * @param roleName    a human-readable label for the role, used in the output matrix
 * @param accessToken a JWT Bearer token for the role
 */
public record RoleAndToken(String roleName, String accessToken) {
	public RoleAndToken {
		Objects.requireNonNull(roleName, "roleName must not be null");
		Objects.requireNonNull(accessToken, "accessToken must not be null");
	}
}
