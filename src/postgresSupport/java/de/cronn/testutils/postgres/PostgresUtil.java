package de.cronn.testutils.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

public class PostgresUtil {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	public PostgresUtil(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
	}

	private void truncateTablesCascade(List<String> tableNamesToTruncate) {
		String tableNames = String.join(", ", tableNamesToTruncate);
		jdbcTemplate.execute("TRUNCATE TABLE " + tableNames + " RESTART IDENTITY CASCADE");
	}

	private List<String> getAllTableNames(String... tablesToExclude) throws SQLException {
		List<String> tableNames = new ArrayList<>();

		Connection connection = DataSourceUtils.getConnection(dataSource);
		try (ResultSet tables =
				connection.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				if (!tableName.toLowerCase(Locale.ROOT).startsWith("databasechangelog")) {
					tableNames.add(tableName);
				}
			}
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}

		for (String tableToExclude : tablesToExclude) {
			if (!tableNames.remove(tableToExclude)) {
				throw new IllegalArgumentException("Table '" + tableToExclude + "' not found");
			}
		}

		Assert.isTrue(!tableNames.isEmpty(), "Found no tables");
		return tableNames.stream().sorted().toList();
	}

	@Transactional
	public void truncateAllTables(String... tablesToExclude) throws SQLException {
		List<String> tableNames = getAllTableNames(tablesToExclude);
		truncateTablesCascade(tableNames);
	}

	/**
	 * Resets all sequences with a non-null {@code last_value} back to 1.
	 *
	 * <p>Returns the names of the sequences that were reset. Pass this list to
	 * {@code HibernateUtil.resetSequenceGeneratorStates()} to keep Hibernate's in-memory
	 * sequence cache in sync. {@code HibernateUtil} is provided by the
	 * {@code de.cronn:test-utils-hibernate-support} Gradle feature variant.
	 *
	 * @return the names of the reset sequences
	 */
	@Transactional
	public List<String> resetAllSequences() {
		List<String> sequenceNames = getSequenceNamesThatNeedToBeReset();
		for (String sequenceName : sequenceNames) {
			resetSequence(sequenceName);
		}
		return sequenceNames;
	}

	private List<String> getSequenceNamesThatNeedToBeReset() {
		return jdbcTemplate.query(
			"select sequencename from pg_sequences where last_value is not null",
			(rs, rowNum) -> rs.getString(1));
	}

	private void resetSequence(String sequenceName) {
		jdbcTemplate.execute("alter sequence " + sequenceName + " restart with 1");
	}

}
