package de.cronn.testutils.authorization;


import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Utility for generating an endpoint authorization matrix as a Markdown table.
 *
 * <p>Discovers all endpoints registered in a {@link RequestMappingHandlerMapping}, calls each one
 * with the provided Bearer tokens, and produces a Markdown table listing which roles had access.
 *
 * <p>The recommended way to obtain an instance is via {@link AuthorizationTestExtension}.
 * For custom adaptations, construct the instance directly:
 * <pre>{@code
 * new AuthorizationTestUtil(handlerMapping, AuthorizationTestUtil.createRestClient(localServerPort))
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

	/**
	 * Create a util for authorization testing
	 *
	 * @param restClient     {@link RestClient} configured against the running application
	 *                       (must have a base URL set, or paths must resolve absolutely)
	 * @param handlerMapping the {@link RequestMappingHandlerMapping} bean of the application
	 */
	public AuthorizationTestUtil(RequestMappingHandlerMapping handlerMapping, RestClient restClient) {
		this.handlerMapping = handlerMapping;
		this.restClient = restClient;
	}

	/**
	 * Like {@link #buildAuthorizationMatrix(List, List)} with no ignored path prefixes.
	 */
	public String buildAuthorizationMatrix(List<RoleAndToken> rolesAndTokens) {
		return buildAuthorizationMatrix(rolesAndTokens, List.of());
	}

	/**
	 * Discovers all endpoints and tests access for each role, returning a Markdown table.
	 *
	 * @param rolesAndTokens      the roles (with Bearer tokens) to test; role names must be unique
	 * @param ignoredPathPrefixes endpoints whose path starts with any of these prefixes are skipped
	 * @return a Markdown table with columns METHOD, PATH, ALLOWED_ROLES
	 */
	public String buildAuthorizationMatrix(
		List<RoleAndToken> rolesAndTokens,
		List<String> ignoredPathPrefixes) {

		validateRolesAndTokens(rolesAndTokens);
		List<Endpoint> endpoints = discoverEndpoints(handlerMapping, ignoredPathPrefixes);
		List<EndpointResult> results = testEndpoints(restClient, endpoints, rolesAndTokens);
		return renderMarkdown(results, allRoleNames(rolesAndTokens));
	}

	/**
	 * Builds an internal {@link RestClient} from {@code baseUrl} with defensive 5s connect / 10s read timeouts.
	 *
	 * @param baseUrl - base URL of the running application (e.g. {@code "http://localhost:8080"});
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
	 * @param localServerPort of the application
	 * @return a rest client that can be used for authorization tests
	 */
	public static RestClient createRestClient(int localServerPort) {
		return createRestClient("http://localhost:" + localServerPort);
	}

	private static void validateRolesAndTokens(List<RoleAndToken> rolesAndTokens) {
		if (rolesAndTokens.isEmpty()) {
			throw new IllegalArgumentException("rolesAndTokens must not be empty");
		}
		Set<String> seen = new LinkedHashSet<>();
		for (RoleAndToken r : rolesAndTokens) {
			if (!seen.add(r.roleName())) {
				throw new IllegalArgumentException("Duplicate role name: " + r.roleName());
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
		List<Endpoint> endpoints,
		List<RoleAndToken> rolesAndTokens) {

		List<EndpointResult> results = new ArrayList<>();
		for (Endpoint endpoint : endpoints) {
			List<String> allowedRoles = new ArrayList<>();
			for (RoleAndToken roleAndToken : rolesAndTokens) {
				HttpStatusCode status = callEndpoint(restClient, endpoint, roleAndToken.accessToken());
				if (isAllowed(status)) {
					allowedRoles.add(roleAndToken.roleName());
				}
			}
			boolean unauthenticatedAllowed = isAllowed(callEndpoint(restClient, endpoint, null));
			results.add(new EndpointResult(endpoint, allowedRoles, unauthenticatedAllowed));
		}
		return results;
	}

	private static HttpStatusCode callEndpoint(
		RestClient restClient, Endpoint endpoint, String accessToken) {
		return restClient.method(endpoint.method())
			.uri(encodePathForRequest(endpoint.path()))
			.headers(headers -> {
				if (accessToken != null) {
					headers.setBearerAuth(accessToken);
				}
			})
			.retrieve()
			.onStatus(status -> true, (req, res) -> {
			})
			.toBodilessEntity()
			.getStatusCode();
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

	private static String renderMarkdown(List<EndpointResult> results, Set<String> allRoleNames) {
		StringBuilder sb = new StringBuilder();
		sb.append("| METHOD | PATH | ALLOWED_ROLES |\n");
		sb.append("| --- | --- | --- |\n");
		for (EndpointResult result : results) {
			String cell = formatAllowedCell(result, allRoleNames);
			sb.append("| ").append(result.endpoint().method().name())
				.append(" | ").append(result.endpoint().path())
				.append(" | ").append(cell)
				.append(" |\n");
		}
		return sb.toString();
	}

	private static String formatAllowedCell(EndpointResult result, Set<String> allRoleNames) {
		List<String> parts = new ArrayList<>();
		if (result.unauthenticatedAllowed()) {
			parts.add("{UNAUTHENTICATED}");
		}
		Set<String> allowed = new LinkedHashSet<>(result.allowedRoles());
		if (allowed.equals(allRoleNames)) {
			parts.add("{ANY_ROLE}");
		} else {
			parts.addAll(allowed);
		}
		return String.join("<br>", parts);
	}

	private static Set<String> allRoleNames(List<RoleAndToken> rolesAndTokens) {
		return rolesAndTokens.stream()
			.map(RoleAndToken::roleName)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private record Endpoint(HttpMethod method, String path) {
	}

	private record EndpointResult(Endpoint endpoint, List<String> allowedRoles, boolean unauthenticatedAllowed) {
	}
}
