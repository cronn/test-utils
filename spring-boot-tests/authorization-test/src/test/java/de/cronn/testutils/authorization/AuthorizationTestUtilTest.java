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
import de.cronn.testutils.authorization.app.DPoPTestProofFactory;
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

	private BearerTokenCredentials bearerFor(Role role) {
		String token = tokenFactory.tokenForRoles(role);
		return new BearerTokenCredentials(role.name(), token);
	}

	private List<Credentials> bearerCredentialsFor(Role... roles) {
		return Arrays.stream(roles).map(r -> (Credentials) bearerFor(r)).toList();
	}

	// --- Bearer-token tests ---

	@Test
	void buildAuthorizationMatrix_withAllThreeRoles(AuthorizationTestUtil authorizationTestUtil) {
		List<Credentials> credentials = bearerCredentialsFor(Role.ADMIN, Role.USER, Role.GUEST);
		Credentials authenticated = new BearerTokenCredentials("authenticated", tokenFactory.tokenForRoles());

		String markdown = authorizationTestUtil.buildAuthorizationMatrix(credentials, authenticated, List.of("/regex"));

		assertWithFile(markdown, FileExtensions.MD);
	}

	@Test
	void buildAuthorizationMatrix_withIgnoredActuatorPrefix(AuthorizationTestUtil authorizationTestUtil) {
		List<Credentials> credentials = bearerCredentialsFor(Role.ADMIN, Role.USER, Role.GUEST);
		Credentials authenticated = new BearerTokenCredentials("authenticated", tokenFactory.tokenForRoles());

		String markdown = authorizationTestUtil.buildAuthorizationMatrix(
			credentials, authenticated, List.of("/actuator", "/error", "/regex"));

		assertWithFile(markdown, FileExtensions.MD);
	}

	@Test
	void buildAuthorizationMatrix_throwsOnRegexConstrainedPathVariable(AuthorizationTestUtil authorizationTestUtil) {
		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(bearerCredentialsFor(Role.ADMIN)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("/regex/{id:[0-9]+}")
			.hasMessageContaining("regex-constrained variable");
	}

	@Test
	void buildAuthorizationMatrix_throwsOnDuplicateNames(AuthorizationTestUtil authorizationTestUtil) {
		String token = tokenFactory.tokenForRoles(Role.USER);
		String token1 = tokenFactory.tokenForRoles(Role.ADMIN);
		List<Credentials> credentials = List.of(
			new BearerTokenCredentials(Role.ADMIN.name(), token1),
			new BearerTokenCredentials(Role.ADMIN.name(), token),
			new BasicAuthCredentials(Role.ADMIN.name(), "admin-user", "admin-password"));

		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(credentials))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Duplicate name: ADMIN");
	}

	@ParameterizedTest
	@ValueSource(ints = { 429, 502, 503, 504 })
	void buildAuthorizationMatrix_throwsOnUnexpectedStatus(int status, AuthorizationTestUtil authorizationTestUtil) {
		SimulatedStatusFilter.simulateStatus(status);

		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(bearerCredentialsFor(Role.ADMIN), List.of("/regex")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(String.valueOf(status));
	}

	@Test
	void buildAuthorizationMatrix_throwsOnEmptyCredentials(AuthorizationTestUtil authorizationTestUtil) {
		assertThatThrownBy(() -> authorizationTestUtil.buildAuthorizationMatrix(List.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must not be empty");
	}

	@Test
	void buildAuthorizationMatrix_withDPoPCredentials(AuthorizationTestUtil authorizationTestUtil) {
		DPoPTestProofFactory adminProofFactory = new DPoPTestProofFactory();
		DPoPTestProofFactory userProofFactory = new DPoPTestProofFactory();
		DPoPTestProofFactory noRoleProofFactory = new DPoPTestProofFactory();

		String accessToken1 = tokenFactory.tokenForRoles(userProofFactory.jwkThumbprint(), Role.USER);
		String accessToken2 = tokenFactory.tokenForRoles(adminProofFactory.jwkThumbprint(), Role.ADMIN);
		List<Credentials> credentials = List.of(
			new DPoPCredentials(Role.ADMIN.name(), accessToken2, adminProofFactory),
			new DPoPCredentials(Role.USER.name(), accessToken1, userProofFactory));
		String accessToken = tokenFactory.tokenForRoles(noRoleProofFactory.jwkThumbprint());
		Credentials authenticated = new DPoPCredentials("authenticated", accessToken, noRoleProofFactory);

		String markdown = authorizationTestUtil.buildAuthorizationMatrix(
			credentials, authenticated, List.of("/actuator", "/error", "/regex"));

		assertWithFile(markdown, FileExtensions.MD);
	}

	@Test
	void buildAuthorizationMatrix_withMixedCredentials(AuthorizationTestUtil authorizationTestUtil) {
		DPoPTestProofFactory guestProofFactory = new DPoPTestProofFactory();

		String accessToken = tokenFactory.tokenForRoles(guestProofFactory.jwkThumbprint(), Role.GUEST);
		List<Credentials> credentials = List.of(
			bearerFor(Role.ADMIN),
			new BasicAuthCredentials(Role.USER.name(), "regular-user", "user-password"),
			new DPoPCredentials(Role.GUEST.name(), accessToken, guestProofFactory));
		Credentials authenticated = new BasicAuthCredentials("authenticated", "no-role-user", "no-role-password");

		String markdown = authorizationTestUtil.buildAuthorizationMatrix(
			credentials, authenticated, List.of("/actuator", "/error", "/regex"));

		assertWithFile(markdown, FileExtensions.MD);
	}
}
