package de.cronn.testutils.authorization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * JUnit 5 extension that resolves {@link AuthorizationTestUtil} as a method parameter.
 *
 * <p>Requires a {@link org.springframework.boot.test.context.SpringBootTest} with
 * {@code webEnvironment = RANDOM_PORT}. Retrieves the {@code requestMappingHandlerMapping} bean
 * and the running server port from the Spring application context automatically.
 *
 * <p>Usage:
 * <pre>{@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @ExtendWith(AuthorizationTestExtension.class)
 * class MyAuthorizationTest {
 *     @Test
 *     void authorizationMatrix(AuthorizationTestUtil authorizationTestUtil) {
 *         String markdown = authorizationTestUtil.buildAuthorizationMatrix(roles, List.of());
 *     }
 * }
 * }</pre>
 */
public class AuthorizationTestExtension implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().getType()
			.equals(AuthorizationTestUtil.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);
		Assertions.assertInstanceOf(ServletWebServerApplicationContext.class, applicationContext);
		ServletWebServerApplicationContext servletWebServerApplicationContext = (ServletWebServerApplicationContext) applicationContext;

		RequestMappingHandlerMapping requestMappingHandlerMapping =
			servletWebServerApplicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
		int localServerPort = servletWebServerApplicationContext.getWebServer().getPort();
		return new AuthorizationTestUtil(requestMappingHandlerMapping, AuthorizationTestUtil.createRestClient(localServerPort));
	}
}
