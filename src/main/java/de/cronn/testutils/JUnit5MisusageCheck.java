package de.cronn.testutils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JUnit5MisusageCheck implements BeforeAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		Class<?> clazz = context.getRequiredTestClass();
		List<Class<?>> topDownHierarchy = getTopDownHierarchy(clazz);
		List<Method> nonCompliantMethods = new ArrayList<>();

		for (Class<? extends Annotation> annotation : Arrays.asList(BeforeEach.class, Test.class, AfterEach.class)) {
			nonCompliantMethods.addAll(checkInstanceMethodMisusage(topDownHierarchy, annotation));
		}
		for (Class<? extends Annotation> annotation : Arrays.asList(BeforeAll.class, AfterAll.class)) {
			nonCompliantMethods.addAll(checkStaticMethodMisusage(topDownHierarchy, annotation));
		}

		if (!nonCompliantMethods.isEmpty()) {
			throw new IllegalStateException(
				nonCompliantMethods.stream()
					.map(Method::toString)
					.collect(
						Collectors.joining(
							"\n",
							"Misused junit5 callback methods: \n",
							""
						)
					)
			);
		}
	}

	private List<Method> checkInstanceMethodMisusage(List<Class<?>> topDownHierarchy, Class<? extends Annotation> annotation) {
		List<Method> misusedMethods = new ArrayList<>();
		List<Method> annotatedMethodsFromAncestors = new ArrayList<>();
		for (Class<?> clazz : topDownHierarchy) {
			List<Method> clazzInstanceMethods = getDeclaredInstanceMethods(clazz);
			for (Method overriddenMethod : findOverriddenMethods(annotatedMethodsFromAncestors, clazzInstanceMethods)) {
				if (overriddenMethod.getAnnotation(annotation) == null) {
					misusedMethods.add(overriddenMethod);
				}
			}
			annotatedMethodsFromAncestors.addAll(getAnnotatedMethods(annotation, clazzInstanceMethods));
		}
		return misusedMethods;
	}

	private List<Method> getDeclaredInstanceMethods(Class<?> clazz) {
		return Stream.of(clazz.getDeclaredMethods())
			.filter(m -> !Modifier.isStatic(m.getModifiers()))
			.collect(Collectors.toList());
	}

	private List<Method> findOverriddenMethods(List<Method> ancestorMethods, List<Method> methodsToCheck) {
		List<Method> overriddenMethods = new ArrayList<>();
		for (Method methodToCheck : methodsToCheck) {
			for (Method ancestorMethod : ancestorMethods) {
				if (isInstanceMethodOverridden(methodToCheck, ancestorMethod)) {
					overriddenMethods.add(methodToCheck);
				}
			}
		}
		return overriddenMethods;
	}

	private List<Method> checkStaticMethodMisusage(List<Class<?>> topDownHierarchy, Class<? extends Annotation> annotation) {
		List<Method> misusedMethods = new ArrayList<>();
		List<Method> annotatedMethodsFromAncestors = new ArrayList<>();
		for (Class<?> clazz : topDownHierarchy) {
			List<Method> clazzDeclaredStaticMethods = getDeclaredStaticMethods(clazz);
			misusedMethods.addAll(findHiddenMethods(annotatedMethodsFromAncestors, clazzDeclaredStaticMethods));
			annotatedMethodsFromAncestors.addAll(getAnnotatedMethods(annotation, clazzDeclaredStaticMethods));
		}
		return misusedMethods;
	}

	private List<Method> getDeclaredStaticMethods(Class<?> clazz) {
		return Stream.of(clazz.getDeclaredMethods())
			.filter(m -> Modifier.isStatic(m.getModifiers()))
			.collect(Collectors.toList());
	}

	private List<Method> findHiddenMethods(List<Method> ancestorMethods, List<Method> methodsToCheck) {
		List<Method> hiddenMethods = new ArrayList<>();
		for (Method methodToCheck : methodsToCheck) {
			for (Method ancestorMethod : ancestorMethods) {
				if (isStaticMethodHidden(methodToCheck, ancestorMethod)) {
					hiddenMethods.add(methodToCheck);
				}
			}
		}
		return hiddenMethods;
	}

	private List<Method> getAnnotatedMethods(Class<? extends Annotation> annotation, List<Method> methods) {
		return methods.stream()
			.filter(method -> method.getAnnotation(annotation) != null)
			.collect(Collectors.toList());
	}

	private boolean isInstanceMethodOverridden(Method childMethod, Method parentMethod) {
		boolean namesMatch = childMethod.getName().equals(parentMethod.getName());
		boolean parameterTypesMatch = Arrays.equals(childMethod.getParameterTypes(), parentMethod.getParameterTypes());
		boolean returnTypesMatch = parentMethod.getReturnType().isAssignableFrom(childMethod.getReturnType());
		return namesMatch && parameterTypesMatch && returnTypesMatch;
	}

	private boolean isStaticMethodHidden(Method childMethod, Method parentMethod) {
		boolean namesMatch = childMethod.getName().equals(parentMethod.getName());
		boolean parameterTypesMatch = Arrays.equals(childMethod.getParameterTypes(), parentMethod.getParameterTypes());
		return namesMatch && parameterTypesMatch;
	}

	private List<Class<?>> getTopDownHierarchy(Class<?> clazz) {
		List<Class<?>> bottomUpHierarchy = new ArrayList<>();
		bottomUpHierarchy.add(clazz);
		while (clazz.getSuperclass() != Object.class) {
			clazz = clazz.getSuperclass();
			bottomUpHierarchy.add(clazz);
		}
		Collections.reverse(bottomUpHierarchy);
		return bottomUpHierarchy;
	}
}
