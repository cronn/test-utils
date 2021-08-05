package de.cronn.testutils;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class JUnitTestExecutor {

	static TestExecutionSummary runTestClassAndReturnSummary(Class<?> testClass) {
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

	static List<Throwable> runTestClassAndReturnExceptionsThrown(Class<?> testClass) {
		return runTestClassAndReturnSummary(testClass)
			.getFailures()
			.stream()
			.map(TestExecutionSummary.Failure::getException)
			.collect(Collectors.toList());
	}
}
