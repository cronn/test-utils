package de.cronn.testutils.logback;

import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import ch.qos.logback.classic.Level;
import de.cronn.assertions.validationfile.FileExtensions;
import de.cronn.assertions.validationfile.junit5.JUnit5ValidationFileAssertions;
import de.cronn.assertions.validationfile.normalization.ValidationNormalizer;
import de.cronn.commons.lang.Action;

// functional-interface overloads (EventFilter/ValidationNormalizer) are intentional
@SuppressWarnings({"overloads", "UnusedReturnValue"})
public interface CapturedLoggingTraits extends JUnit5ValidationFileAssertions {

	default String capturedLoggerName() {
		return Logger.ROOT_LOGGER_NAME;
	}

	default RenderingOptions capturedLoggingRenderingOptions() {
		return new RenderingOptions(true, 40, true);
	}

	default ValidationNormalizer defaultValidationNormalizerForCapturedLogging() {
		return input -> input;
	}

	default EventFilter defaultCapturedLoggingEventFilter() {
		return EventFilter.all();
	}

	default Level defaultCapturedLoggingLevel() {
		return Level.DEBUG;
	}

	default void beforeCapturedConsoleLogging() {
	}

	default LogbackCaptor getLogbackCaptor() {
		return getLogbackCaptor(defaultCapturedLoggingEventFilter(), defaultCapturedLoggingLevel());
	}

	default LogbackCaptor getLogbackCaptor(EventFilter filter, Level level) {
		return new LogbackCaptor(
			capturedLoggerName(),
			capturedLoggingRenderingOptions(),
			defaultCapturedLoggingEventFilter().and(filter),
			level);
	}

	// --- Action overloads ---

	default void withCapturedConsoleLogging(Action action) {
		withCapturedConsoleLogging(action.toCallable());
	}

	default void withCapturedConsoleLogging(Action action, ValidationNormalizer normalizer) {
		withCapturedConsoleLogging(action.toCallable(), normalizer);
	}

	default void withCapturedConsoleLogging(Action action, String suffix) {
		withCapturedConsoleLogging(action.toCallable(), suffix);
	}

	default void withCapturedConsoleLogging(Action action, ValidationNormalizer normalizer, String suffix) {
		withCapturedConsoleLogging(action.toCallable(), normalizer, suffix);
	}

	default void withCapturedConsoleLogging(Action action, EventFilter filter) {
		withCapturedConsoleLogging(action.toCallable(), filter);
	}

	default void withCapturedConsoleLogging(Action action, EventFilter filter, ValidationNormalizer normalizer) {
		withCapturedConsoleLogging(action.toCallable(), filter, normalizer);
	}

	default void withCapturedConsoleLogging(Action action, EventFilter filter, String suffix) {
		withCapturedConsoleLogging(action.toCallable(), filter, suffix);
	}

	default void withCapturedConsoleLogging(
			Action action, EventFilter filter, ValidationNormalizer normalizer, String suffix) {
		withCapturedConsoleLogging(action.toCallable(), filter, normalizer, suffix);
	}

	default void withCapturedConsoleLogging(Action action, Level level) {
		withCapturedConsoleLogging(action.toCallable(), level);
	}

	default void withCapturedConsoleLogging(Action action, Level level, ValidationNormalizer normalizer) {
		withCapturedConsoleLogging(action.toCallable(), level, normalizer);
	}

	default void withCapturedConsoleLogging(Action action, Level level, String suffix) {
		withCapturedConsoleLogging(action.toCallable(), level, suffix);
	}

	default void withCapturedConsoleLogging(
			Action action, Level level, ValidationNormalizer normalizer, String suffix) {
		withCapturedConsoleLogging(action.toCallable(), level, normalizer, suffix);
	}

	// --- Callable overloads ---

	default <T> T withCapturedConsoleLogging(Callable<T> callable) {
		return withCapturedConsoleLogging(callable, defaultValidationNormalizerForCapturedLogging());
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, ValidationNormalizer normalizer) {
		return withCapturedConsoleLogging(callable, normalizer, "logging");
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, String suffix) {
		return withCapturedConsoleLogging(callable, defaultValidationNormalizerForCapturedLogging(), suffix);
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, ValidationNormalizer normalizer, String suffix) {
		return withCapturedConsoleLogging(callable, defaultCapturedLoggingEventFilter(), normalizer, suffix);
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, EventFilter filter) {
		return withCapturedConsoleLogging(callable, filter, defaultValidationNormalizerForCapturedLogging());
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, EventFilter filter, ValidationNormalizer normalizer) {
		return withCapturedConsoleLogging(callable, filter, normalizer, "logging");
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, EventFilter filter, String suffix) {
		return withCapturedConsoleLogging(callable, filter, defaultValidationNormalizerForCapturedLogging(), suffix);
	}

	default <T> T withCapturedConsoleLogging(
			Callable<T> callable, EventFilter filter, ValidationNormalizer normalizer, String suffix) {
		return withCapturedConsoleLogging(callable, filter, defaultCapturedLoggingLevel(), normalizer, suffix);
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, Level level) {
		return withCapturedConsoleLogging(callable, level, defaultValidationNormalizerForCapturedLogging());
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, Level level, ValidationNormalizer normalizer) {
		return withCapturedConsoleLogging(callable, level, normalizer, "logging");
	}

	default <T> T withCapturedConsoleLogging(Callable<T> callable, Level level, String suffix) {
		return withCapturedConsoleLogging(callable, level, defaultValidationNormalizerForCapturedLogging(), suffix);
	}

	default <T> T withCapturedConsoleLogging(
			Callable<T> callable, Level level, ValidationNormalizer normalizer, String suffix) {
		return withCapturedConsoleLogging(callable, defaultCapturedLoggingEventFilter(), level, normalizer, suffix);
	}

	default <T> T withCapturedConsoleLogging(
			Callable<T> callable, EventFilter filter, Level level, ValidationNormalizer normalizer, String suffix) {
		beforeCapturedConsoleLogging();

		LogbackCaptor captor = getLogbackCaptor(filter, level);

		try {
			T result = captor.captureLoggingDuring(callable);
			String capturedLog = captor.getCapturedLog().collect(Collectors.joining("\n"));
			assertWithFileWithSuffix(capturedLog, normalizer, suffix, FileExtensions.TXT);
			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
