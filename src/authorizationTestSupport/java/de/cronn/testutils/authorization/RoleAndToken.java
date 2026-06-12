package de.cronn.testutils.authorization;

import java.util.Objects;

/**
 * Associates a role name with a bearer access token to be used during authorization testing.
 *
 * <p>Each instance represents one role to probe. The token is sent as
 * {@code Authorization: Bearer <accessToken>} and must grant <em>only</em> the permissions
 * described by {@code roleName} — mixing multiple roles in a single token produces misleading
 * results in the output matrix.
 *
 * @param roleName    a human-readable label for the role, used as the column header in the output matrix (e.g. {@code "ADMIN"}, {@code "USER"})
 * @param accessToken a bearer token (e.g. a JWT or opaque token) that is authorised for exactly this role
 */
public record RoleAndToken(String roleName, String accessToken) {
	public RoleAndToken {
		Objects.requireNonNull(roleName, "roleName must not be null");
		Objects.requireNonNull(accessToken, "accessToken must not be null");
	}
}
