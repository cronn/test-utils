package de.cronn.testutils.postgres;

import static org.assertj.core.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import de.cronn.testutils.hibernate.HibernateUtil;
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
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.3");

	@Autowired
	PostgresUtil postgresUtil;

	@Autowired
	HibernateUtil hibernateUtil;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	EntityManager entityManager;

	@Autowired
	TransactionUtil transactionUtil;

	@BeforeEach
	void cleanDatabase() throws SQLException {
		postgresUtil.truncateAllTables();
		List<String> resetSequences = postgresUtil.resetAllSequences();
		if (!resetSequences.isEmpty()) {
			hibernateUtil.resetSequenceGeneratorStates(resetSequences);
		}
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

	@Test
	void step03_assertEntityIdsStartAtOneAfterFullReset() throws Exception {
		// Fill DB so that Hibernate's internal sequence cache is warmed up
		transactionUtil.doInTransaction(() -> {
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
		});

		// Reset DB and Hibernate's in-memory state
		postgresUtil.truncateAllTables();
		postgresUtil.resetAllSequences();
		hibernateUtil.resetSequenceGeneratorStates();

		SampleEntity entity = transactionUtil.doInTransactionWithResult(() -> {
			SampleEntity e = new SampleEntity();
			entityManager.persist(e);
			entityManager.flush();
			return e;
		});
		assertThat(entity.getId()).isEqualTo(1L);
	}

	private int countRows(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
	}

	private long nextSequenceValue(String sequence) {
		return jdbcTemplate.queryForObject("select last_value from " + sequence, Long.class);
	}

}
