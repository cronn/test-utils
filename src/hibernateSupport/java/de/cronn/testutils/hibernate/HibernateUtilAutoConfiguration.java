package de.cronn.testutils.hibernate;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

import jakarta.persistence.EntityManagerFactory;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnSingleCandidate(EntityManagerFactory.class)
public class HibernateUtilAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HibernateUtil hibernateUtil(EntityManagerFactory entityManagerFactory) {
		return new HibernateUtil(entityManagerFactory);
	}

}
