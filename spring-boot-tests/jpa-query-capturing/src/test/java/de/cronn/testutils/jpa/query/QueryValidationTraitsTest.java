package de.cronn.testutils.jpa.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Override
	public QueryCaptor getQueryCaptor() {
		return queryCapturing;
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
