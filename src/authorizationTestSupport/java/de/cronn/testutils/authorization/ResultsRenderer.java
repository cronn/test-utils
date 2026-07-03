package de.cronn.testutils.authorization;

import java.util.Collection;
import java.util.List;

/**
 * Renders the endpoint results collected by {@link AuthorizationTestUtil} into an authorization matrix.
 *
 * <p>The default implementation is {@link MarkdownTableRenderer}; provide a custom implementation to
 * produce a different output format.
 */
public interface ResultsRenderer {

	/**
	 * @param results     one entry per discovered endpoint, in stable order
	 * @param credentials the principals the endpoints were tested with
	 * @return the rendered authorization matrix
	 */
	String render(List<EndpointResult> results, Collection<? extends Credentials> credentials);
}
