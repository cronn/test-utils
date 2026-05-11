package de.cronn.testutils.jpa.query;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NoOpSqlFormatterConfig {

	@Bean
	@ConditionalOnMissingBean
	SqlQueryFormatter noOpSqlQueryFormatter() {
		return query -> query;
	}

}
