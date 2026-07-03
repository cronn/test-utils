package de.cronn.testutils.authorization;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Access-check outcome for a single {@link Endpoint}.
 *
 * @param endpoint               the endpoint that was tested
 * @param allowedRoles           names of the credentials that were granted access
 * @param unauthenticatedAllowed whether the endpoint was accessible without any credentials
 * @param authenticatedAllowed   whether authenticated-but-unprivileged access was granted,
 *                               or {@code null} if that check was skipped
 */
public record EndpointResult(Endpoint endpoint, List<String> allowedRoles, boolean unauthenticatedAllowed,
                             @Nullable Boolean authenticatedAllowed) {
}
