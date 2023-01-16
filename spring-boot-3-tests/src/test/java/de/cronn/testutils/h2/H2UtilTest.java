package de.cronn.testutils.h2;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import de.cronn.testutils.h2.app.Application;
import de.cronn.testutils.h2.app.SampleEntity;
import de.cronn.testutils.h2.app.SampleTableGeneratedEntity;
import de.cronn.testutils.h2.app.SecondSchemaEntity;
import de.cronn.testutils.h2.app.TransactionUtil;
import jakarta.persistence.EntityManager;

@ExtendWith(SoftAssertionsExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(classes = Application.class)
@Import(H2Util.class)
public class H2UtilTest {

	@Autowired
	H2Util h2Util;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	TransactionUtil transactionUtil;

	@Autowired
	EntityManager entityManager;

	@InjectSoftAssertions
	SoftAssertions softly;

	@AfterEach
	void resetDatabase() {
		h2Util.resetDatabase();
	}

	@Test
	void step01_fillDatabase() {
		transactionUtil.doInTransaction(() -> {
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleTableGeneratedEntity());
			entityManager.persist(new SampleTableGeneratedEntity());
			entityManager.persist(new SecondSchemaEntity());
		});

		softly.assertThat(countTables()).isEqualTo(5);
		softly.assertThat(countRows("public.sample_entity")).isEqualTo(3);
		softly.assertThat(countRows("public.sample_table_generated_entity")).isEqualTo(2);
		softly.assertThat(countRows("public.sample_generator")).isEqualTo(1);
		softly.assertThat(countRows("second_schema.second_schema_entity")).isEqualTo(1);
		softly.assertThat(countRows("second_schema.second_generator")).isEqualTo(1);
		softly.assertThat(jdbcTemplate.queryForObject("select next_val from public.sample_generator", Integer.class)).isEqualTo(2);
		softly.assertThat(jdbcTemplate.queryForObject("select next_val from second_schema.second_generator", Integer.class)).isEqualTo(1);
	}

	@Test
	void step02_assertDatabaseEmpty() {
		softly.assertThat(countTables()).isEqualTo(5);
		softly.assertThat(countRows("public.sample_entity")).isEqualTo(0);
		softly.assertThat(countRows("public.sample_table_generated_entity")).isEqualTo(0);
		softly.assertThat(countRows("public.sample_generator")).isEqualTo(1);
		softly.assertThat(countRows("second_schema.second_schema_entity")).isEqualTo(0);
		softly.assertThat(countRows("second_schema.second_generator")).isEqualTo(1);
		softly.assertThat(jdbcTemplate.queryForObject("select next_val from public.sample_generator", Integer.class)).isEqualTo(0);
		softly.assertThat(jdbcTemplate.queryForObject("select next_val from second_schema.second_generator", Integer.class)).isEqualTo(0);

		transactionUtil.doInTransaction(() -> {
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleTableGeneratedEntity());
			entityManager.persist(new SecondSchemaEntity());
		});

		softly.assertThat(jdbcTemplate.queryForObject("select id from public.sample_entity", Integer.class)).isEqualTo(1);
		softly.assertThat(jdbcTemplate.queryForObject("select id from public.sample_table_generated_entity", Integer.class)).isEqualTo(1);
		softly.assertThat(jdbcTemplate.queryForObject("select id from second_schema.second_schema_entity", Integer.class)).isEqualTo(1);
	}

	@Test
	void step03_testDrop() {
		h2Util.dropAllObjects();
		softly.assertThat(countTables()).isEqualTo(0);
	}

	int countRows(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
	}

	int countTables() {
		return jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_type = 'TABLE'", Integer.class);
	}

}
