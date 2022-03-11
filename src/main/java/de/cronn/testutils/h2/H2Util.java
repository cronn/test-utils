package de.cronn.testutils.h2;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TableGenerator;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public class H2Util {

	private static final Logger log = LoggerFactory.getLogger(H2Util.class);

	private static final String H2_JDBC_DRIVER = "H2 JDBC Driver";

	private static final Map<Class<?>, List<TableGenerator>> TABLE_GENERATORS = new LinkedHashMap<>();

	@Autowired(required = false)
	private EntityManager entityManager;

	@Autowired(required = false)
	private DataSource dataSource;

	public void resetDatabase() {
		if (dataSource != null) {
			try {
				Set<Table> sequenceTableNames = collectSequenceTableNames();
				cleanupEmbeddedDatabase(dataSource, sequenceTableNames);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void dropAllObjects() {
		if (dataSource != null) {
			try {
				dropAllObjects(dataSource);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Set<Table> collectSequenceTableNames() throws Exception {
		if (entityManager == null) {
			return Collections.emptySet();
		}
		String defaultSchema = getDefaultSchema();
		Set<Table> sequenceTableNames = new LinkedHashSet<>();
		for (EntityType<?> entityType : entityManager.getMetamodel().getEntities()) {
			Class<?> entityJavaType = entityType.getJavaType();
			for (TableGenerator entry : TABLE_GENERATORS.computeIfAbsent(entityJavaType, H2Util::getTableGeneratorAnnotations)) {
				if ("".equals(entry.table())) {
					throw new UnsupportedOperationException("Empty TableGenerator table name is not supported. Please specify table name explicitly");
				}
				String schema = entry.schema();
				sequenceTableNames.add(new Table(entry.table(), schema == null || schema.isEmpty() ? defaultSchema : schema));
			}
		}
		return sequenceTableNames;
	}

	private String getDefaultSchema() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			return connection.getSchema();
		}
	}

	private static List<TableGenerator> getTableGeneratorAnnotations(Class<?> type) {
		Set<Field> fields = new LinkedHashSet<>();
		collectFields(type, fields);
		return fields.stream()
			.filter(f -> f.isAnnotationPresent(TableGenerator.class))
			.map(f -> f.getAnnotation(TableGenerator.class))
			.collect(Collectors.toList());
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

	private static void cleanupEmbeddedDatabase(DataSource dataSource, Set<Table> sequenceTableNames) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertIsH2Database(connection);
			truncateAllTables(connection, sequenceTableNames);
			resetAllSequences(connection);
		}
	}

	private static void dropAllObjects(DataSource dataSource) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertIsH2Database(connection);
			dropAllObjects(connection);
		}
	}

	private static void resetAllSequences(Connection connection) throws Exception {
		List<String> sequenceNames = new ArrayList<>();
		try (PreparedStatement stmt = connection.prepareStatement("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE CURRENT_VALUE > 0")) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				while (resultSet.next()) {
					String sequenceName = resultSet.getString("SEQUENCE_NAME");
					sequenceNames.add(sequenceName);
				}
			}
		}
		for (String sequenceName : sequenceNames) {
			executeStatement(connection, "ALTER SEQUENCE " + sequenceName + " RESTART WITH 1");
		}
	}

	private static void dropAllObjects(Connection connection) throws Exception {
		executeStatement(connection, "DROP ALL OBJECTS");
	}

	private static void truncateAllTables(Connection connection, Set<Table> sequencesTableNames) throws Exception {
		executeStatement(connection, "SET REFERENTIAL_INTEGRITY FALSE");

		Set<String> lowerCaseSequencesTableNames = collectInLowerCase(sequencesTableNames);
		Set<Table> tableNames = getTableNames(connection);
		for (Table table : tableNames) {
			long count = selectCount(connection, table);
			if (count > 0) {
				String tableIdentifierSql = table.toSql();
				if (lowerCaseSequencesTableNames.contains(tableIdentifierSql.toLowerCase())) {
					log.warn("resetting {} sequences in table '{}'", count, tableIdentifierSql);
					executeStatement(connection, "UPDATE " + tableIdentifierSql + " SET next_val = 0");
				} else {
					log.warn("deleting {} rows from table '{}'", count, tableIdentifierSql);
					executeStatement(connection, "TRUNCATE TABLE " + tableIdentifierSql);
				}
			}
		}

		executeStatement(connection, "SET REFERENTIAL_INTEGRITY TRUE");
	}

	private static Set<String> collectInLowerCase(Set<Table> sequencesTableNames) {
		return sequencesTableNames.stream()
			.map(Table::toSql)
			.map(String::toLowerCase)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static long selectCount(Connection connection, Table table) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table.toSql())) {
			try (ResultSet resultSet = statement.executeQuery()) {
				Assert.isTrue(resultSet.next());
				return resultSet.getLong(1);
			}
		}
	}

	public static Set<Table> getTableNames(Connection con) throws SQLException {
		String[] tableTypes = { "TABLE" };
		Set<Table> tableNames = new LinkedHashSet<>();
		try (ResultSet tables = con.getMetaData().getTables(null, null, null, tableTypes)) {
			while (tables.next()) {
				String schema = tables.getString("TABLE_SCHEM");
				String tableName = tables.getString("TABLE_NAME");
				tableNames.add(new Table(tableName, schema));
			}
		}
		return tableNames;
	}

	private static void assertIsH2Database(Connection connection) throws SQLException {
		String driverName = connection.getMetaData().getDriverName();
		Assert.state(H2_JDBC_DRIVER.equals(driverName), "Unexpected driver: " + driverName);
	}

	private static void executeStatement(Connection connection, String sql) throws Exception {
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.execute();
		}
	}

	public static class Table {
		private final String name;
		private final String schema;

		public Table(String name, String schema) {
			this.name = name;
			this.schema = schema;
		}

		public String getName() {
			return name;
		}

		public String getSchema() {
			return schema;
		}

		public String toSql() {
			return schema + "." + name;
		}
	}

}
