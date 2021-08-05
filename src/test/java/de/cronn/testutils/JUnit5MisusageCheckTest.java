package de.cronn.testutils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

class JUnit5MisusageCheckTest {

	static final String FLAG = "Junit5MisusageCheckTest";
	static final String FLAG_ENABLED_VALUE = "true";

	@BeforeEach
	void enableTestCaseTestClasses() {
		System.setProperty(FLAG, FLAG_ENABLED_VALUE);
	}

	@AfterEach
	void disableTestCaseTestClasses() {
		System.clearProperty(FLAG);
	}

	@Test
	void testMisusageCheck() {
		TestExecutionSummary result = runTestClass(SampleChildTest.class);
		Assertions.assertThat(result.getFailures())
			.hasOnlyOneElementSatisfying(
				failure ->
					Assertions.assertThat(failure.getException())
						.hasMessage(
							"Misused junit5 callback methods: \n" +
							"void de.cronn.testutils.SampleChildTest.beforeEach()\n" +
							"void de.cronn.testutils.SampleChildTest.test()\n" +
							"void de.cronn.testutils.SampleChildTest.afterEach()\n" +
							"static void de.cronn.testutils.SampleChildTest.beforeAll()\n" +
							"static void de.cronn.testutils.SampleChildTest.afterAll()"
						)
			);
	}

	private TestExecutionSummary runTestClass(Class<?> testClass) {
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		LauncherFactory
			.create()
			.execute(
				LauncherDiscoveryRequestBuilder.request()
					.selectors(DiscoverySelectors.selectClass(testClass))
					.build(),
				listener
			);
		return listener.getSummary();
	}
}

@ExtendWith(JUnit5MisusageCheck.class)
@EnabledIfSystemProperty(named = JUnit5MisusageCheckTest.FLAG, matches = JUnit5MisusageCheckTest.FLAG_ENABLED_VALUE)
class SampleParentTest {
	@BeforeEach
	void beforeEach() {
	}

	@AfterEach
	void afterEach() {
	}

	@BeforeAll
	static void beforeAll() {
	}

	@AfterAll
	static void afterAll() {
	}

	@Test
	void test() {
	}
}

@EnabledIfSystemProperty(named = JUnit5MisusageCheckTest.FLAG, matches = JUnit5MisusageCheckTest.FLAG_ENABLED_VALUE)
class SampleChildTest extends SampleParentTest {
	@Override
	void beforeEach() {
	}

	@Override
	void afterEach() {
	}

	static void beforeAll() {
	}

	static void afterAll() {
	}

	@Override
	void test() {
	}

	@Test
	void childTest() {
	}
}
