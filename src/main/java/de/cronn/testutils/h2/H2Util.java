package de.cronn.testutils.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

public class H2Util {

	private static final Logger log = LoggerFactory.getLogger(H2Util.class);

	private static final String H2_JDBC_DRIVER = "H2 JDBC Driver";

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired(required = false)
	private DataSource dataSource;

	public void resetDatabase() {
		if (dataSource != null) {
			try {
				JpaUtil jpaUtil = new JpaUtil(applicationContext);
				Collection<Table> sequenceTableNames = jpaUtil.collectSequenceTableNames();
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

	private static void cleanupEmbeddedDatabase(DataSource dataSource, Collection<Table> sequenceTableNames) throws Exception {
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

	private static void truncateAllTables(Connection connection, Collection<Table> sequencesTableNames) throws Exception {
		executeStatement(connection, "SET REFERENTIAL_INTEGRITY FALSE");
		String defaultSchema = connection.getSchema();
		Set<String> lowerCaseSequencesTableNames = collectInLowerCase(sequencesTableNames, defaultSchema);
		Set<Table> tableNames = getTableNames(connection);
		for (Table table : tableNames) {
			long count = selectCount(connection, table);
			if (count > 0) {
				String tableIdentifierSql = table.toSql(defaultSchema);
				if (lowerCaseSequencesTableNames.contains(tableIdentifierSql.toLowerCase())) {
					log.warn("resetting {} sequences in table '{}'", count, tableIdentifierSql);
					executeStatement(connection, "UPDATE " + tableIdentifierSql + " SET next_val = 0");
				} else {
					log.warn("deleting {} rows from table '{}'", count, tableIdentifierSql);
					executeStatement(connection, "TRUNCATE TABLE " + tableIdentifierSql + " restart identity");
				}
			}
		}

		executeStatement(connection, "SET REFERENTIAL_INTEGRITY TRUE");
	}

	private static Set<String> collectInLowerCase(Collection<Table> sequencesTableNames, String defaultSchema) {
		return sequencesTableNames.stream()
			.map(table -> table.toSql(defaultSchema))
			.map(String::toLowerCase)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static long selectCount(Connection connection, Table table) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table.toSql(connection.getSchema()))) {
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

		public String toSql(String defaultSchema) {
			return (schema == null ? defaultSchema : schema) + "." + name;
		}
	}

}
