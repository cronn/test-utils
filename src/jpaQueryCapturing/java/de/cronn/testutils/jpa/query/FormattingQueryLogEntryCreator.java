package de.cronn.testutils.jpa.query;

import java.util.List;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;

public class FormattingQueryLogEntryCreator extends DefaultQueryLogEntryCreator {
	private final SqlQueryFormatter sqlQueryFormatter;

	public FormattingQueryLogEntryCreator(SqlQueryFormatter sqlQueryFormatter) {
		setMultiline(true);
		this.sqlQueryFormatter = sqlQueryFormatter;
	}

	@Override
	protected void writeTimeEntry(
		StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		/* Time entry changes, we don't like changes in validation files */
	}

	@Override
	protected String formatQuery(String query) {
		return this.sqlQueryFormatter.format(query);
	}
}
