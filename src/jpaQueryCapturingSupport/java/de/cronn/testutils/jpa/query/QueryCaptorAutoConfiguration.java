package de.cronn.testutils.jpa.query;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import net.ttddyy.dsproxy.listener.logging.QueryLogEntryCreator;

@AutoConfiguration
@Import({
	HibernateSqlQueryFormatterConfig.class,
	NoOpSqlFormatterConfig.class
})
class QueryCaptorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean // let users define their own implementation
	QueryLogEntryCreator formattingQueryLogEntryCreator(SqlQueryFormatter sqlQueryFormatter) {
		return new FormattingQueryLogEntryCreator(sqlQueryFormatter);
	}

	@Bean
	@ConditionalOnMissingBean // let users define their own implementation
	QueryCapturingListener queryCapturingListener() {
		return new QueryCapturingListener();
	}

	@Bean
	@ConditionalOnMissingBean // let users define their own implementation
	QueryCaptor queryCaptor(QueryCapturingListener queryCapturingListener, QueryLogEntryCreator queryLogEntryCreator) {
		return new QueryCaptor(queryCapturingListener, queryLogEntryCreator);
	}

}
