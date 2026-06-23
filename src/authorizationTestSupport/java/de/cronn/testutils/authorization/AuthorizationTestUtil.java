package de.cronn.testutils.authorization;


import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Utility for generating an endpoint authorization matrix as a Markdown table.
 *
 * <p>Discovers all endpoints registered in a {@link RequestMappingHandlerMapping}, calls each one
 * with the provided credentials, and produces a Markdown table listing which principals had access.
 *
 * <p>Both bearer-token and HTTP Basic credentials are supported and may be mixed freely in a
 * single matrix — both authentication schemes ultimately resolve to Spring {@code GrantedAuthority}
 * on the server side.
 *
 * <p>The recommended way to obtain an instance is via {@link AuthorizationTestExtension}.
 * For custom adaptations, construct the instance directly:
 * <pre>{@code
 * // bearer / basic auth only:
 * new AuthorizationTestUtil(handlerMapping, AuthorizationTestUtil.createRestClient(localServerPort))
 *
 * // with DPoP support:
 * String baseUrl = AuthorizationTestUtil.localBaseUrl(localServerPort);
 * new AuthorizationTestUtil(handlerMapping, AuthorizationTestUtil.createRestClient(baseUrl), baseUrl)
 * }</pre>
 */
public final class AuthorizationTestUtil {

	private static final String DEFAULT_PATH_PARAM_VALUE = "__AUTH_TEST__";

	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

	private static final Set<RequestMethod> ALL_METHODS_EXCEPT_TRACE =
		EnumSet.complementOf(EnumSet.of(RequestMethod.TRACE));

	private final RequestMappingHandlerMapping handlerMapping;
	private final RestClient restClient;
	@Nullable
	private final String baseUrl;

	/**
	 * Create a util for authorization testing.
	 *
	 * <p>Sufficient for bearer-token and HTTP Basic authentication. If you intend to use
	 * {@link DPoPCredentials}, use
	 * {@link #AuthorizationTestUtil(RequestMappingHandlerMapping, RestClient, String)} instead
	 * so that full absolute URIs can be constructed for DPoP proof generation.
	 *
	 * @param handlerMapping the {@link RequestMappingHandlerMapping} bean of the application
	 * @param restClient     {@link RestClient} configured against the running application
	 *                       (must have a base URL set, or paths must resolve absolutely)
	 */
	public AuthorizationTestUtil(RequestMappingHandlerMapping handlerMapping, RestClient restClient) {
		this(handlerMapping, restClient, null);
	}

	/**
	 * Create a util for authorization testing, with an explicit base URL for DPoP proof generation.
	 *
	 * <p>Required when using {@link DPoPCredentials}. For bearer-token and
	 * HTTP Basic authentication the two-argument constructor is sufficient.
	 *
	 * @param handlerMapping the {@link RequestMappingHandlerMapping} bean of the application
	 * @param restClient     {@link RestClient} configured against the running application
	 *                       (must have a base URL set, or paths must resolve absolutely)
	 * @param baseUrl        the base URL of the running application (e.g. {@code "http://localhost:8080"}),
	 *                       used to construct full absolute URIs for DPoP proof generation
	 */
	public AuthorizationTestUtil(RequestMappingHandlerMapping handlerMapping, RestClient restClient, String baseUrl) {
		this.handlerMapping = handlerMapping;
		this.restClient = restClient;
		this.baseUrl = baseUrl;
	}

	/**
	 * Like {@link #buildAuthorizationMatrix(Collection, List)} with no ignored path prefixes.
	 */
	public String buildAuthorizationMatrix(Collection<? extends Credentials> credentials) {
		return buildAuthorizationMatrix(credentials, List.of());
	}

	/**
	 * Like {@link #buildAuthorizationMatrix(Collection, Credentials, List)} without an authenticated credentials check.
	 */
	public String buildAuthorizationMatrix(Collection<? extends Credentials> credentials, List<String> ignoredPathPrefixes) {
		return buildAuthorizationMatrix(credentials, null, ignoredPathPrefixes);
	}

	/**
	 * Like {@link #buildAuthorizationMatrix(Collection, Credentials, List)} with no ignored path prefixes.
	 */
	public String buildAuthorizationMatrix(Collection<? extends Credentials> credentials, @Nullable Credentials authenticatedCredentials) {
		return buildAuthorizationMatrix(credentials, authenticatedCredentials, List.of());
	}

	/**
	 * Discovers all endpoints and tests access for each principal, returning a Markdown table.
	 *
	 * <p>In addition to testing each named principal, this optionally checks access with credentials
	 * that are authenticated but carry no roles (e.g. a no-roles JWT or a basic-auth user with no
	 * granted authorities). Pass {@code null} to skip this check. When the endpoint is accessible,
	 * the result renders as {@code {AUTHENTICATED}} in the matrix.
	 *
	 * @param credentials              the principals (with their credentials) to test; names must be unique
	 * @param authenticatedCredentials credentials for an authenticated but unprivileged caller, or {@code null} to skip
	 * @param ignoredPathPrefixes      endpoints whose path starts with any of these prefixes are skipped
	 * @return a Markdown table with columns METHOD, PATH, ALLOWED_ROLES
	 */
	public String buildAuthorizationMatrix(
		Collection<? extends Credentials> credentials,
		@Nullable Credentials authenticatedCredentials,
		List<String> ignoredPathPrefixes) {

		validateCredentials(credentials);
		List<Endpoint> endpoints = discoverEndpoints(handlerMapping, ignoredPathPrefixes);
		List<EndpointResult> results = testEndpoints(restClient, baseUrl, endpoints, credentials, authenticatedCredentials);
		return renderMarkdown(results, allNames(credentials));
	}

	/**
	 * Builds an internal {@link RestClient} from {@code baseUrl} with defensive 5s connect / 10s read timeouts.
	 *
	 * @param baseUrl base URL of the running application (e.g. {@code "http://localhost:8080"});
	 *                paths discovered from {@code handlerMapping} are appended to it
	 * @return a rest client that can be used for authorization tests
	 */
	public static RestClient createRestClient(String baseUrl) {
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(DEFAULT_CONNECT_TIMEOUT)
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(DEFAULT_READ_TIMEOUT);
		return RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
	}

	/**
	 * Like {@link #createRestClient(String)} but targets {@code http://localhost:<localServerPort>}
	 * — convenient for use with {@code @LocalServerPort}.
	 *
	 * @param localServerPort the port of the running application
	 * @return a rest client that can be used for authorization tests
	 */
	public static RestClient createRestClient(int localServerPort) {
		return createRestClient(localBaseUrl(localServerPort));
	}

	/**
	 * Returns the base URL for the given local server port: {@code http://localhost:<localServerPort>}.
	 * Needed when constructing {@link AuthorizationTestUtil} with DPoP support.
	 *
	 * @param localServerPort the port of the running application
	 * @return the base URL string
	 */
	public static String localBaseUrl(int localServerPort) {
		return "http://localhost:" + localServerPort;
	}

	private static void validateCredentials(Collection<? extends Credentials> credentials) {
		if (credentials.isEmpty()) {
			throw new IllegalArgumentException("credentials must not be empty");
		}
		Set<String> seen = new LinkedHashSet<>();
		for (Credentials c : credentials) {
			if (!seen.add(c.name())) {
				throw new IllegalArgumentException("Duplicate name: " + c.name());
			}
		}
	}

	private static List<Endpoint> discoverEndpoints(
		RequestMappingHandlerMapping handlerMapping, List<String> ignoredPathPrefixes) {

		Set<Endpoint> endpoints = new LinkedHashSet<>();
		for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
			Set<String> paths = getPaths(info, ignoredPathPrefixes);
			if (paths.isEmpty()) {
				continue;
			}
			Set<RequestMethod> methods = getRequestMethods(info);
			for (String path : paths) {
				for (RequestMethod method : methods) {
					endpoints.add(new Endpoint(method.asHttpMethod(), path));
				}
			}
		}
		return endpoints.stream()
			.sorted(Comparator.comparing(Endpoint::path).thenComparing(e -> e.method().name()))
			.toList();
	}

	private static Set<RequestMethod> getRequestMethods(RequestMappingInfo info) {
		Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
		if (methods.isEmpty()) {
			// Endpoint in Spring RestController without request method constraint is callable with all request methods.
			// TRACE is excluded: most servers (incl. Tomcat by default) block it at the connector level for security
			// reasons (see RFC 7231 §4.3.8 and Cross-Site Tracing), so requests never reach the auth chain.
			return ALL_METHODS_EXCEPT_TRACE;
		}
		return methods;
	}

	private static Set<String> getPaths(RequestMappingInfo info, List<String> ignoredPathPrefixes) {
		PathPatternsRequestCondition pathCondition = info.getPathPatternsCondition();
		if (pathCondition == null) {
			throw new IllegalStateException("RequestMappingInfo has no PathPatternsCondition");
		}
		return pathCondition.getPatternValues().stream()
			.filter(path -> ignoredPathPrefixes.stream().noneMatch(path::startsWith))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String encodePathForRequest(String path) {
		if (path.matches(".*\\{[^/{}]+:.*")) {
			throw new IllegalStateException(
				"Path '" + path + "' uses a regex-constrained variable (e.g. '{id:[0-9]+}'). "
					+ "AuthorizationTestUtil substitutes a fixed placeholder for all path variables "
					+ "and cannot guarantee it satisfies the constraint. "
					+ "Add this path to ignoredPathPrefixes or remove the regex constraint.");
		}
		return path.replaceAll("\\{[^/{}]+}", DEFAULT_PATH_PARAM_VALUE);
	}

	private static List<EndpointResult> testEndpoints(
		RestClient restClient,
		String baseUrl,
		List<Endpoint> endpoints,
		Collection<? extends Credentials> credentials,
		@Nullable Credentials authenticatedCredentials) {

		return endpoints.stream()
			.map(endpoint -> testEndpoint(restClient, baseUrl, endpoint, credentials, authenticatedCredentials))
			.toList();
	}

	private static EndpointResult testEndpoint(
		RestClient restClient,
		String baseUrl,
		Endpoint endpoint,
		Collection<? extends Credentials> credentials,
		@Nullable Credentials authenticatedCredentials) {

		if (isUnauthenticatedAllowed(restClient, baseUrl, endpoint)) {
			return new EndpointResult(endpoint, List.of(), true, null);
		}
		Boolean authenticatedAllowed = checkAuthenticatedAccess(restClient, baseUrl, endpoint, authenticatedCredentials);
		if (Boolean.TRUE.equals(authenticatedAllowed)) {
			return new EndpointResult(endpoint, List.of(), false, true);
		}
		return new EndpointResult(endpoint, checkAccess(restClient, baseUrl, endpoint, credentials), false, authenticatedAllowed);
	}

	private static boolean isUnauthenticatedAllowed(RestClient restClient, String baseUrl, Endpoint endpoint) {
		return isAllowed(callEndpoint(restClient, baseUrl, endpoint, null));
	}

	@Nullable
	private static Boolean checkAuthenticatedAccess(RestClient restClient, String baseUrl, Endpoint endpoint, @Nullable Credentials authenticatedCredentials) {
		if (authenticatedCredentials == null) {
			return null;
		}
		return isAllowed(callEndpoint(restClient, baseUrl, endpoint, authenticatedCredentials));
	}

	private static List<String> checkAccess(RestClient restClient, String baseUrl, Endpoint endpoint, Collection<? extends Credentials> credentials) {
		return credentials.stream()
			.filter(c -> isAllowed(callEndpoint(restClient, baseUrl, endpoint, c)))
			.map(Credentials::name)
			.toList();
	}

	private static HttpStatusCode callEndpoint(
		RestClient restClient, @Nullable String baseUrl, Endpoint endpoint, @Nullable Credentials credentials) {
		String encodedPath = encodePathForRequest(endpoint.path());
		return restClient.method(endpoint.method())
			.uri(encodedPath)
			.headers(headers -> {
				if (credentials != null) {
					URI uri = resolveUri(baseUrl, encodedPath, credentials);
					credentials.applyTo(headers, endpoint.method(), uri);
				}
			})
			.retrieve()
			.onStatus(status -> true, (req, res) -> {
			})
			.toBodilessEntity()
			.getStatusCode();
	}

	private static URI resolveUri(@Nullable String baseUrl, String encodedPath, Credentials credentials) {
		if (credentials instanceof DPoPCredentials) {
			if (baseUrl == null) {
				throw new IllegalStateException(
					"baseUrl is required when using DPoP credentials. Use AuthorizationTestUtil(handlerMapping, restClient, baseUrl) to provide it."
				);
			}
			return URI.create(baseUrl + encodedPath);
		}
		return URI.create(encodedPath);
	}

	private static boolean isAllowed(HttpStatusCode statusCode) {
		if (statusCode.is2xxSuccessful()) {
			return true;
		}
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		if (status == null) {
			throw new IllegalStateException("Unexpected status code: " + statusCode.value());
		}
		return switch (status) {
			case UNAUTHORIZED, FORBIDDEN, METHOD_NOT_ALLOWED -> false;
			case TOO_MANY_REQUESTS, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
				throw new IllegalStateException("Unexpected status code: " + statusCode.value());
			default -> true;
		};
	}

	private static String renderMarkdown(List<EndpointResult> results, Set<String> allNames) {
		StringBuilder sb = new StringBuilder();
		sb.append("| METHOD | PATH | ALLOWED_ROLES |\n");
		sb.append("| --- | --- | --- |\n");
		for (EndpointResult result : results) {
			String cell = formatAllowedCell(result, allNames);
			sb.append("| ").append(result.endpoint().method().name())
				.append(" | ").append(result.endpoint().path())
				.append(" | ").append(cell)
				.append(" |\n");
		}
		return sb.toString();
	}

	private static String formatAllowedCell(EndpointResult result, Set<String> allNames) {
		if (result.unauthenticatedAllowed()) {
			return "{⚠ PERMIT_ALL ⚠}";
		}
		if (Boolean.TRUE.equals(result.authenticatedAllowed())) {
			return "{AUTHENTICATED}";
		}
		Set<String> allowed = new LinkedHashSet<>(result.allowedRoles());
		if (allowed.equals(allNames)) {
			return "{ANY_ROLE}";
		} else {
			return String.join("<br>", allowed);
		}
	}

	private static Set<String> allNames(Collection<? extends Credentials> credentials) {
		return credentials.stream()
			.map(Credentials::name)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private record Endpoint(HttpMethod method, String path) {
	}

	private record EndpointResult(Endpoint endpoint, List<String> allowedRoles, boolean unauthenticatedAllowed,
	                              @Nullable Boolean authenticatedAllowed) {
	}
}
