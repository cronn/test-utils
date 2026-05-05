package de.cronn.testutils.h2;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import jakarta.persistence.EntityManager;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnSingleCandidate(DataSource.class)
public class H2UtilAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public H2Util h2Util(EntityManager entityManager, DataSource dataSource) {
		return new H2Util(entityManager, dataSource);
	}

}
