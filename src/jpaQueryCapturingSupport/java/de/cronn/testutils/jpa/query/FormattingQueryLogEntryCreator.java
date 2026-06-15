package de.cronn.testutils.jpa.query;

import java.util.List;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;

public class FormattingQueryLogEntryCreator extends DefaultQueryLogEntryCreator {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final SqlQueryFormatter sqlQueryFormatter;

	public FormattingQueryLogEntryCreator(SqlQueryFormatter sqlQueryFormatter) {
		this.sqlQueryFormatter = sqlQueryFormatter;
	}

	@Override
	public String getLogEntry(ExecutionInfo execInfo, List<QueryInfo> queryInfoList, boolean writeDataSourceName, boolean writeConnectionId, boolean writeIsolation) {
		final StringBuilder sb = new StringBuilder();

		sb.append(LINE_SEPARATOR);
		sb.append("-- ");

		if (writeDataSourceName) {
			writeDataSourceNameEntry(sb, execInfo, queryInfoList);
		}

		if (writeConnectionId) {
			writeConnectionIdEntry(sb, execInfo, queryInfoList);
		}

		if (writeIsolation) {
			writeIsolationEntry(sb, execInfo, queryInfoList);
		}

		writeResultEntry(sb, execInfo, queryInfoList);

		sb.delete(sb.length() - 2, sb.length());  // delete last ", "
		sb.append(LINE_SEPARATOR);
		sb.append("-- ");

		writeTypeEntry(sb, execInfo, queryInfoList);

		writeBatchEntry(sb, execInfo, queryInfoList);

		writeQuerySizeEntry(sb, execInfo, queryInfoList);

		writeBatchSizeEntry(sb, execInfo, queryInfoList);

		sb.delete(sb.length() - 2, sb.length());  // delete last ", "
		sb.append(LINE_SEPARATOR);
		sb.append("-- ");

		writeParamsEntry(sb, execInfo, queryInfoList);

		writeQueriesEntry(sb, execInfo, queryInfoList);

		return sb.toString();
	}

	@Override
	protected String formatQuery(String query) {
		return this.sqlQueryFormatter.format(query);
	}

	@Override
	protected void writeQueriesEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		for (QueryInfo queryInfo : queryInfoList) {
			sb.append(formatQuery(queryInfo.getQuery()));
			sb.append(';');
			sb.append(LINE_SEPARATOR);
		}
	}

}
