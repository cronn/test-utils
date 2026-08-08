package de.cronn.testutils.logback;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;

class CapturedLoggingTraitsTest implements CapturedLoggingTraits {

	private static final Logger log = LoggerFactory.getLogger("sample.logger");
	private static final Logger classLog = LoggerFactory.getLogger(CapturedLoggingTraitsTest.class);

	@Test
	void singleMessage() {
		withCapturedConsoleLogging(() -> log.info("Hello world"));
	}

	@Test
	void multipleLevels() {
		withCapturedConsoleLogging(() -> {
			log.debug("a debug message");
			log.info("an info message");
			log.warn("a warning message");
			log.error("an error message");
		});
	}

	@Test
	void multilineMessage() {
		withCapturedConsoleLogging(() -> {
			log.info("first line\nsecond line");
			log.info("another message");
		});
	}

	@Test
	void withException() {
		withCapturedConsoleLogging(() ->
			log.error("Something went wrong", new IllegalStateException("boom")));
	}

	@Test
	void multipleBlocksWithSuffix() {
		withCapturedConsoleLogging(() -> log.info("first block"), "first");
		withCapturedConsoleLogging(() -> log.info("second block"), "second");
	}

	@Test
	void onlyWarningsAndAbove() {
		withCapturedConsoleLogging(() -> {
			log.info("this is filtered out");
			log.warn("this is captured");
			log.error("this too");
		}, EventFilter.atLeastWarning());
	}

	@Test
	void capturedFromGivenLevel() {
		withCapturedConsoleLogging(() -> {
			log.debug("filtered out debug");
			log.info("captured info");
		}, Level.INFO);
	}

	@Test
	@SuppressWarnings({"try", "unused"})
	void withMdc() {
		withCapturedConsoleLogging(() -> {
			try (MDC.MDCCloseable requestId = MDC.putCloseable("requestId", "abc-123");
					MDC.MDCCloseable user = MDC.putCloseable("user", "alice")) {
				log.info("handling request");
				log.warn("almost done");
			}
		});
	}

	@Test
	void onlyFromGivenClass() {
		withCapturedConsoleLogging(() -> {
			log.info("from sample.logger, filtered out");
			classLog.info("from the test class, captured");
		}, EventFilter.forClass(CapturedLoggingTraitsTest.class));
	}

	@Test
	void actionExceptionIsPropagated() {
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> withCapturedConsoleLogging(() -> {
				throw new IllegalStateException("failing action");
			}))
			.withMessage("failing action");
	}

	@Test
	@SuppressWarnings({"try", "unused"})
	void captureLogsAsList() throws Exception {
		LogbackCaptor captor = getLogbackCaptor();
		captor.captureLoggingDuring(() -> {
			log.info("This is informative");
			try (MDC.MDCCloseable userId = MDC.putCloseable("user.id", "123")) {
				log.warn("This is a warning for a user");
			}
		});

		// Check that the MDC value is present with an assertion
		assertThat(captor.getCapturedLoggingEvents())
			.anyMatch(it -> it.getMDCPropertyMap().containsKey("user.id"));
	}

}
