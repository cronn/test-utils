package de.cronn.testutils.jpa.query;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import de.cronn.testutils.jpa.query.app.SampleEntity;
import jakarta.persistence.EntityManager;

class QueryCaptorTest extends BaseIntegrationTest {

	@Autowired
	QueryCaptor queryCapturing;

	@Autowired
	EntityManager entityManager;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Test
	void capturesQueriesWhileListening() throws Exception {
		transactionTemplate.executeWithoutResult(tx -> entityManager.persist(new SampleEntity()));

		queryCapturing.captureQueriesDuring(() -> {
			transactionTemplate.executeWithoutResult(tx -> entityManager.find(SampleEntity.class, 1L));
			transactionTemplate.executeWithoutResult(tx -> entityManager.find(SampleEntity.class, 2L));
		});

		List<String> captured = queryCapturing.getCapturedQueries();
		assertThat(captured).containsExactly(
			"""
				Name:dataSource, Isolation:NONE, Success:True
				Type:Prepared, Batch:False, QuerySize:1, BatchSize:0
				Query:["
				    select
				        se1_0.id\s
				    from
				        sample_entity se1_0\s
				    where
				        se1_0.id=?"]
				Params:[(1)]""",
			"""
				Name:dataSource, Isolation:NONE, Success:True
				Type:Prepared, Batch:False, QuerySize:1, BatchSize:0
				Query:["
				    select
				        se1_0.id\s
				    from
				        sample_entity se1_0\s
				    where
				        se1_0.id=?"]
				Params:[(2)]"""
		);
	}

	@Test
	void doesNotCaptureQueriesWhenNotListening() {
		transactionTemplate.executeWithoutResult(tx -> entityManager.persist(new SampleEntity()));

		List<String> captured = queryCapturing.getCapturedQueries();
		assertThat(captured).isEmpty();
	}

	@Test
	void clearsQueriesOnStartListening() throws Exception {
		queryCapturing.captureQueriesDuring(() -> transactionTemplate.executeWithoutResult(tx -> entityManager.find(SampleEntity.class, 1L)));

		queryCapturing.captureQueriesDuring(() -> {
			// noop
		});

		assertThat(queryCapturing.getCapturedQueries()).isEmpty();
	}

	@Test
	void throwsOnNestedCapturing() {
		assertThatThrownBy(() ->
			queryCapturing.captureQueriesDuring(() ->
				queryCapturing.captureQueriesDuring(() -> {})
			)
		).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("nested capturing is not supported");
	}
}
