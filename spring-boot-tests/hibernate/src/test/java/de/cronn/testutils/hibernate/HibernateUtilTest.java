package de.cronn.testutils.hibernate;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.cronn.testutils.h2.H2Util;
import de.cronn.testutils.hibernate.app.Application;
import de.cronn.testutils.hibernate.app.SampleEntity;
import de.cronn.testutils.hibernate.app.TransactionUtil;
import jakarta.persistence.EntityManager;

@SpringBootTest(classes = Application.class)
class HibernateUtilTest {

	@Autowired
	H2Util h2Util;

	@Autowired
	HibernateUtil hibernateUtil;

	@Autowired
	EntityManager entityManager;

	@Autowired
	TransactionUtil transactionUtil;

	@BeforeEach
	void cleanDatabase() {
		h2Util.resetDatabase();
		hibernateUtil.resetSequenceGeneratorStates();
	}

	@Test
	void resetSequenceGeneratorStates_resetsHibernateCacheAfterSequenceReset() {
		// Fill DB so that Hibernate's internal sequence cache is warmed up
		transactionUtil.doInTransaction(() -> {
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
			entityManager.persist(new SampleEntity());
		});

		// Reset DB sequences and Hibernate's in-memory state
		h2Util.resetDatabase();
		hibernateUtil.resetSequenceGeneratorStates();

		// After reset, newly persisted entities should receive IDs starting from 1 again
		SampleEntity entity = transactionUtil.doInTransactionWithResult(() -> {
			SampleEntity e = new SampleEntity();
			entityManager.persist(e);
			entityManager.flush();
			return e;
		});

		assertThat(entity.getId()).isEqualTo(1L);
	}

}
