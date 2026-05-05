package de.cronn.testutils.postgres;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration(after = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@ConditionalOnSingleCandidate(DataSource.class)
public class PostgresUtilAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PostgresUtil postgresUtil(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		return new PostgresUtil(dataSource, jdbcTemplate);
	}

}
