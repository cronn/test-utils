package de.cronn.testutils.h2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class JpaUtil {

	private static final String JAKARTA_PERSISTENCE_PACKAGE = "jakarta.persistence";
	private static final String JAVAX_PERSISTENCE_PACKAGE = "javax.persistence";
	private static final boolean USE_JAKARTA_PERSISTENCE = isClassPresent(JAKARTA_PERSISTENCE_PACKAGE + ".EntityManager");
	private static final boolean USE_JAVAX_PERSISTENCE = isClassPresent(JAVAX_PERSISTENCE_PACKAGE + ".EntityManager");

	private static final Map<Class<?>, List<H2Util.Table>> SEQUENCE_TABLES = new LinkedHashMap<>();

	private final SpelExpressionParser expressionParser = new SpelExpressionParser();

	private final ApplicationContext applicationContext;

	public JpaUtil(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	Collection<H2Util.Table> collectSequenceTableNames() {
		Optional<Class<Object>> entityManagerClass = getPersistenceClass("EntityManager");
		if (!entityManagerClass.isPresent() || applicationContext.getBeanNamesForType(entityManagerClass.get()).length == 0) {
			return Collections.emptyList();
		}
		Set<H2Util.Table> sequenceTableNames = new LinkedHashSet<>();
		for (Class<?> jpaEntityClass : getJpaEntityClasses()) {
			List<H2Util.Table> sequenceTablesForEntity = SEQUENCE_TABLES.computeIfAbsent(
				jpaEntityClass,
				JpaUtil::getSequenceTablesForEntity
			);
			sequenceTableNames.addAll(sequenceTablesForEntity);
		}
		return sequenceTableNames;
	}

	private static boolean isClassPresent(String fqn) {
		try {
			Class.forName(fqn);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<Class<T>> getPersistenceClass(String classSimpleName) {
		try {
			if (USE_JAKARTA_PERSISTENCE) {
				return Optional.of((Class<T>) Class.forName(JAKARTA_PERSISTENCE_PACKAGE + "." + classSimpleName));
			} else if (USE_JAVAX_PERSISTENCE) {
				return Optional.of((Class<T>) Class.forName(JAVAX_PERSISTENCE_PACKAGE + "." + classSimpleName));
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return Optional.empty();
	}

	private static <T> Class<T> getPersistenceClassOrThrow(String classSimpleName) {
		Optional<Class<T>> persistenceClass = getPersistenceClass(classSimpleName);
		return persistenceClass.orElseThrow(IllegalStateException::new);
	}

	@SuppressWarnings("unchecked")
	private List<Class<?>> getJpaEntityClasses() {
		Object entityManager = applicationContext.getBean(getPersistenceClassOrThrow("EntityManager"));
		Object value = expressionParser.parseExpression("metamodel.entities.![javaType]").getValue(entityManager);
		return (List<Class<?>>) value;
	}

	private static List<H2Util.Table> getSequenceTablesForEntity(Class<?> type) {
		Class<? extends Annotation> tableGeneratorAnnotationClass = getPersistenceClassOrThrow("TableGenerator");
		Set<Field> fields = new LinkedHashSet<>();
		collectFields(type, fields);
		List<? extends Annotation> tableGeneratorAnnotations = fields.stream()
			.filter(f -> f.isAnnotationPresent(tableGeneratorAnnotationClass))
			.map(f -> f.getAnnotation(tableGeneratorAnnotationClass))
			.collect(Collectors.toList());
		List<H2Util.Table> sequenceTables = new ArrayList<>();
		for (Annotation tableGeneratorAnnotation : tableGeneratorAnnotations) {
			String schema = invokeStringGetter(tableGeneratorAnnotation, "schema");
			String table = invokeStringGetter(tableGeneratorAnnotation, "table");
			if ("".equals(table)) {
				throw new UnsupportedOperationException("Empty TableGenerator table name is not supported. Please specify table name explicitly");
			}
			sequenceTables.add(new H2Util.Table(table, schema == null || schema.isEmpty() ? null : schema));
		}
		return sequenceTables;
	}

	private static void collectFields(Class<?> type, Collection<Field> collectedFields) {
		collectedFields.addAll(Arrays.asList(type.getFields()));
		collectedFields.addAll(Arrays.asList(type.getDeclaredFields()));
		if (!type.equals(Object.class)) {
			Class<?> superclass = type.getSuperclass();
			if (superclass != null) {
				collectFields(superclass, collectedFields);
			}
		}
	}

	private static String invokeStringGetter(Object o, String getterName) {
		try {
			Method getter = o.getClass().getDeclaredMethod(getterName);
			return (String) getter.invoke(o);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

}
