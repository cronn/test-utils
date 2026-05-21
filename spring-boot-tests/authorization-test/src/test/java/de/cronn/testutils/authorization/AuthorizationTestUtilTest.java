package de.cronn.testutils.authorization;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.cronn.assertions.validationfile.FileExtensions;
import de.cronn.assertions.validationfile.junit5.JUnit5ValidationFileAssertions;
import de.cronn.testutils.authorization.app.JwtTestTokenFactory;
import de.cronn.testutils.authorization.app.Role;
import de.cronn.testutils.authorization.app.SimulatedStatusFilter;
import de.cronn.testutils.authorization.app.TestApplication;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(AuthorizationTestExtension.class)
class AuthorizationTestUtilTest implements JUnit5ValidationFileAssertions {

	@Autowired
	private JwtTestTokenFactory tokenFactory;

	@BeforeEach
	void clearSimulatedStatus() {
		SimulatedStatusFilter.reset();
	}

	private String tokenWithNoRoles() {
		return tokenFactory.tokenForRoles();
	}

	private RoleAndToken roleAndToken(Role role) {
		return new RoleAndToken(role.name(), tokenFactory.tokenForRoles(role));
	}

	private List<RoleAndToken> rolesAndTokensFor(Role... roles) {
		return Arrays.stream(roles).map(this::roleAndToken).toList();
	}

	@Test
	void buildAuthorizationMatrix_withAllThreeRoles(AuthorizationTestUtil authorizationTestUtil) {
		List<RoleAndToken> roles = rolesAndTokensFor(Role.ADMIN, Role.USER, Role.GUEST);

		String markdown = authorizationTestUtil.buildAuthorizationMatrix(roles, tokenWithNoRoles(), List.of("/regex"));

		assertWithFile(markdown, FileExtensions.MD);
	}

	@Test
	void buildAuthorizationMatrix_withIgnoredActuatorPrefix(AuthorizationTestUtil authorizationTestUtil) {
		List<RoleAndToken> roles = rolesAndTokensFor(Role.ADMIN, Role.USER, Role.GUEST);

		String markdown = authorizationTestUtil.buildAuthorizationMatrix(
			roles, tokenWithNoRoles(), List.of("/actuator", "/error", "/regex"));

		assertWithFile(markdown, FileExtensions.MD);
	}

	@Test
	void buildAuthorizationMatrix_throwsOnRegexConstrainedPathVariable(AuthorizationTestUtil authorizationTestUtil) {
		List<RoleAndToken> roles = rolesAndTokensFor(Role.ADMIN);

		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(roles))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("/regex/{id:[0-9]+}")
			.hasMessageContaining("regex-constrained variable");
	}

	@Test
	void buildAuthorizationMatrix_throwsOnDuplicateRoleNames(AuthorizationTestUtil authorizationTestUtil) {
		List<RoleAndToken> roles = List.of(
			new RoleAndToken(Role.ADMIN.name(), tokenFactory.tokenForRoles(Role.ADMIN)),
			new RoleAndToken(Role.ADMIN.name(), tokenFactory.tokenForRoles(Role.USER)));

		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(roles))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Duplicate role name: ADMIN");
	}

	@ParameterizedTest
	@ValueSource(ints = { 429, 502, 503, 504 })
	void buildAuthorizationMatrix_throwsOnUnexpectedStatus(int status, AuthorizationTestUtil authorizationTestUtil) {
		SimulatedStatusFilter.simulateStatus(status);
		List<RoleAndToken> roles = rolesAndTokensFor(Role.ADMIN);

		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(roles, List.of("/regex")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(String.valueOf(status));
	}

	@Test
	void buildAuthorizationMatrix_throwsOnEmptyRoles(AuthorizationTestUtil authorizationTestUtil) {
		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(List.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must not be empty");
	}
}
