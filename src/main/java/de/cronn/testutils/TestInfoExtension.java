package de.cronn.testutils;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestInfoExtension implements BeforeEachCallback, TestInfo {

	private String displayName;
	private Set<String> tags;
	private Class<?> testClass;
	private Method testMethod;

	@Override
	public void beforeEach(ExtensionContext context) {
		this.displayName = context.getDisplayName();
		this.tags = context.getTags();
		this.testClass = context.getTestClass().orElse(null);
		this.testMethod = context.getTestMethod().orElse(null);
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public Set<String> getTags() {
		return tags;
	}

	@Override
	public Optional<Class<?>> getTestClass() {
		return Optional.ofNullable(testClass);
	}

	@Override
	public Optional<Method> getTestMethod() {
		return Optional.ofNullable(testMethod);
	}

}
