package de.cronn.testutils.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.cronn.testutils.postgres.app.Application;
import de.cronn.testutils.postgres.app.SampleEntity;
import de.cronn.testutils.postgres.app.TransactionUtil;
import jakarta.persistence.EntityManager;

@Testcontainers
@SpringBootTest(classes = Application.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class PostgresUtilTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.3");

	@Autowired
	PostgresUtil postgresUtil;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	EntityManager entityManager;

	@Autowired
	TransactionUtil transactionUtil;

	@AfterEach
	void cleanDatabase() throws SQLException {
		postgresUtil.truncateAllTables();
		postgresUtil.resetAllSequences();
	}

	@Test
	void step01_fillDatabase() {
		transactionUtil.doInTransaction(() -> {
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
		});

		assertThat(countRows("sample_entity")).isEqualTo(3);
		assertThat(nextSequenceValue("sample_entity_seq")).isGreaterThan(1L);

		List<String> resetSequences = postgresUtil.resetAllSequences();
		assertThat(resetSequences).contains("sample_entity_seq");
		assertThat(nextSequenceValue("sample_entity_seq")).isEqualTo(1L);
	}

	@Test
	void step02_assertDatabaseIsClean() {
		assertThat(countRows("sample_entity")).isEqualTo(0);
		assertThat(nextSequenceValue("sample_entity_seq")).isEqualTo(1L);
	}

	private int countRows(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
	}

	private long nextSequenceValue(String sequence) {
		return jdbcTemplate.queryForObject("select last_value from " + sequence, Long.class);
	}

}
