package de.cronn.testutils.jpa.query;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionTemplate;

import de.cronn.testutils.jpa.query.app.ChildEntity;
import de.cronn.testutils.jpa.query.app.SampleEntity;
import jakarta.persistence.EntityManager;

class QueryValidationTraitsTest extends BaseIntegrationTest implements QueryValidationTraits {

	@Autowired
	QueryCaptor queryCapturing;

	@Autowired
	EntityManager entityManager;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Autowired
	DataSource dataSource;

	@Override
	public QueryCaptor getQueryCaptor() {
		return queryCapturing;
	}

	@Test
	void failedQuery() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() ->
				captureQueryAndCompareWithFile(() ->
					transactionTemplate.executeWithoutResult(tx -> {
						Connection connection = DataSourceUtils.getConnection(dataSource);
						try (Statement statement = connection.createStatement()) {
							statement.execute("select 1 from does_not_exist");
						} catch (SQLException e) {
							throw new RuntimeException(e);
						} finally {
							DataSourceUtils.releaseConnection(connection, dataSource);
						}
					})
				))
			.withRootCauseInstanceOf(SQLException.class)
			.withMessageContaining("Table \"DOES_NOT_EXIST\" not found");
	}

	@Test
	void multipleQueries() {
		captureQueryAndCompareWithFile(() ->
			transactionTemplate.executeWithoutResult(tx -> {
				Connection connection = DataSourceUtils.getConnection(dataSource);
				try (Statement statement = connection.createStatement()) {
					statement.addBatch("insert into sample_entity (id) values (1)");
					statement.addBatch("insert into child_entity (name, parent_id, id) values ('Child', 1, 1)");
					statement.executeBatch();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				} finally {
					DataSourceUtils.releaseConnection(connection, dataSource);
				}
			}));
	}

	@Test
	void persist() {
		Long id = captureQueryAndCompareWithFile(() -> transactionTemplate.execute(tx -> {
			SampleEntity entity = new SampleEntity();
			ChildEntity child = new ChildEntity();
			child.setName("Some Child");
			entity.addChild(child);
			entityManager.persist(entity);
			return entity.getId();
		}));
		assertThat(id).isEqualTo(1);
	}

	@Test
	void persistAndFind() {
		captureQueryAndCompareWithFile(() ->
			transactionTemplate.executeWithoutResult(tx -> {
				SampleEntity entity = new SampleEntity();
				for (int i = 0; i < 5; i++) {
					ChildEntity child = new ChildEntity();
					child.setName("Child " + (i + 1));
					entity.addChild(child);
				}
				entityManager.persist(entity);
			}), "persist");

		captureQueryAndCompareWithFile(() ->
			transactionTemplate.executeWithoutResult(tx -> {
				SampleEntity sampleEntity = entityManager.find(SampleEntity.class, 1L);
				assertThat(sampleEntity.getChildren()).hasSize(5);
			})
		);
	}

}
