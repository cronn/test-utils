package de.cronn.testutils.jpa.query;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(FormatStyle.class)
class HibernateSqlQueryFormatterConfig {

	@Bean
	@ConditionalOnMissingBean // let users define their own
	public SqlQueryFormatter sqlQueryFormatter() {
		Formatter formatter = FormatStyle.BASIC.getFormatter();
		return formatter::format;
	}

}
