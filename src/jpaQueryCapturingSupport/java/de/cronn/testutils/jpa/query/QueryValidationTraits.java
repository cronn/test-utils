package de.cronn.testutils.jpa.query;

import java.util.concurrent.Callable;

import de.cronn.assertions.validationfile.junit5.JUnit5ValidationFileAssertions;
import de.cronn.assertions.validationfile.normalization.ValidationNormalizer;
import de.cronn.commons.lang.Action;

public interface QueryValidationTraits extends JUnit5ValidationFileAssertions {

	String SUFFIX_SQL = "sql";

	QueryCaptor getQueryCaptor();

	default void captureQueryAndCompareWithFile(Action action) {
		captureQueryAndCompareWithFile(action.toCallable());
	}

	default <T> T captureQueryAndCompareWithFile(Callable<T> callable) {
		return captureQueryAndCompareWithFile(callable, SUFFIX_SQL);
	}

	default void captureQueryAndCompareWithFile(Action action, String suffix) {
		captureQueryAndCompareWithFile(action.toCallable(), suffix);
	}

	default void captureQueryAndCompareWithFile(Action action, ValidationNormalizer normalizer) {
		captureQueryAndCompareWithFile(action.toCallable(), normalizer);
	}

	default void captureQueryAndCompareWithFile(
		Action action, ValidationNormalizer normalizer, String suffix) {
		captureQueryAndCompareWithFile(action.toCallable(), normalizer, suffix);
	}

	default <T> T captureQueryAndCompareWithFile(
		Callable<T> callable, ValidationNormalizer normalizer) {
		return captureQueryAndCompareWithFile(callable, normalizer, SUFFIX_SQL);
	}

	default <T> T captureQueryAndCompareWithFile(Callable<T> callable, String suffix) {
		return captureQueryAndCompareWithFile(
			callable, defaultValidationNormalizerForQueryCaptor(), suffix);
	}

	default ValidationNormalizer defaultValidationNormalizerForQueryCaptor() {
		return new ByteArrayReplacer();
	}

	default <T> T captureQueryAndCompareWithFile(
		Callable<T> callable, ValidationNormalizer normalizer, String suffix) {
		QueryCaptor queryListener = getQueryCaptor();
		try {
			T result = queryListener.captureQueriesDuring(callable);
			String capturedQueries = String.join("\n\n\n", queryListener.getCapturedQueries());
			assertWithFileWithSuffix(capturedQueries, normalizer, suffix);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
