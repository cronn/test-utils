package de.cronn.testutils.hibernate;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnSingleCandidate(EntityManagerFactory.class)
public class HibernateUtilAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HibernateUtil hibernateUtil(EntityManagerFactory entityManagerFactory) {
		return new HibernateUtil(entityManagerFactory);
	}

}
