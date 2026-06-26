package de.cronn.testutils.hibernate;

import java.util.Collection;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import de.cronn.reflection.util.PropertyUtils;
import jakarta.persistence.EntityManagerFactory;

/**
 * Resets Hibernate's internal sequence generator state to stay in sync with the database.
 *
 * <p>After resetting database sequences externally (e.g., via
 * {@code ALTER SEQUENCE ... RESTART WITH 1}), Hibernate's in-memory sequence cache may be
 * out of sync, leading to duplicate key errors or unexpected ID values. This class resets
 * the internal state of all {@link SequenceStyleGenerator} instances so that Hibernate
 * fetches the next values directly from the database on the next allocation.
 *
 * <p><strong>Note:</strong> This class accesses internal Hibernate APIs via reflection
 * (using {@code de.cronn:reflection-util}). Test thoroughly after Hibernate upgrades,
 * as internal field names may change.
 *
 * <p>As an alternative, you can disable Hibernate's sequence value caching entirely by setting
 * {@code spring.jpa.properties.hibernate.id.optimizer.pooled.preferred=NONE} in
 * {@code application.properties}. This avoids the need for this reset at the cost of one
 * additional sequence query per entity insertion.
 *
 * @see <a href="https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html#settings-mapping">Hibernate settings: hibernate.id.optimizer.pooled.preferred</a>
 */
public class HibernateUtil {

	private final EntityManagerFactory entityManagerFactory;

	public HibernateUtil(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Resets the in-memory state of all Hibernate sequence generators, but only if
	 * {@code resetSequences} is non-empty.
	 *
	 * <p>Convenience overload intended for use with the list returned by
	 * {@link de.cronn.testutils.postgres.PostgresUtil#resetAllSequences()}: if no sequences
	 * were reset, there is nothing to synchronize.
	 */
	public void resetSequenceGeneratorStates(Collection<String> resetSequences) {
		if (!resetSequences.isEmpty()) {
			resetSequenceGeneratorStates();
		}
	}

	/**
	 * Resets the in-memory state of all Hibernate sequence generators.
	 *
	 * <p>Call this after resetting database sequences to ensure Hibernate fetches fresh
	 * values from the database instead of serving stale cached values.
	 */
	public void resetSequenceGeneratorStates() {
		SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
		sessionFactory.getMappingMetamodel()
			.forEachEntityDescriptor(entityPersister -> {
				Generator generator = entityPersister.getGenerator();
				if (generator instanceof SequenceStyleGenerator sequenceStyleGenerator) {
					if (sequenceStyleGenerator.getOptimizer() instanceof PooledOptimizer pooledOptimizer) {
						resetInternalOptimizerState(pooledOptimizer);
					}
				}
			});
	}

	private static void resetInternalOptimizerState(PooledOptimizer optimizer) {
		PropertyUtils.writeDirectly(optimizer, "noTenantState", null);
	}

}
