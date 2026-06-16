package de.cronn.testutils.jpa.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.dsproxy.listener.logging.AbstractQueryLogEntryCreator;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

public class FormattingQueryLogEntryCreator extends AbstractQueryLogEntryCreator {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final SqlQueryFormatter sqlQueryFormatter;

	public FormattingQueryLogEntryCreator(SqlQueryFormatter sqlQueryFormatter) {
		this.sqlQueryFormatter = sqlQueryFormatter;
	}

	@Override
	public String getLogEntry(ExecutionInfo execInfo, List<QueryInfo> queries, boolean writeDataSourceName, boolean writeConnectionId, boolean writeIsolation) {
		List<String> lines = new ArrayList<>();

		lines.add(formatMetadata(execInfo, queries));

		for (QueryInfo query : queries) {
			lines.add(formatQuery(execInfo, query));
		}

		return String.join(LINE_SEPARATOR, lines);
	}

	private String formatQuery(ExecutionInfo executionInfo, QueryInfo query) {
		List<String> lines = new ArrayList<>();
		String formattedQuery = sqlQueryFormatter.format(query.getQuery());
		lines.add(addSuffixIfMissing(formattedQuery, ";"));

		List<String> formattedParameters = new ArrayList<>();
		for (List<ParameterSetOperation> parameters : query.getParametersList()) {
			if (!parameters.isEmpty()) {
				formattedParameters.add(parameters.stream()
					.map(param -> formatParams(executionInfo, param))
					.collect(Collectors.joining(", ", "(", ")")));
			}
		}

		if (!formattedParameters.isEmpty()) {
			lines.add(formattedParameters.stream()
				.collect(Collectors.joining(LINE_SEPARATOR + "--         ", "-- Params: ", "")));
		}

		return String.join(LINE_SEPARATOR, lines);
	}

	private static String addSuffixIfMissing(String value, String suffix) {
		if (!value.stripTrailing().endsWith(suffix)) {
			return value + suffix;
		} else {
			return value;
		}
	}

	private String formatMetadata(ExecutionInfo execInfo, List<QueryInfo> queryInfos) {
		String name = execInfo.getDataSourceName();
		StringBuilder sb = new StringBuilder();
		sb.append("-- Name: ").append(name == null ? "" : name);
		sb.append(", Isolation: ").append(getTransactionIsolation(execInfo.getIsolationLevel()));
		sb.append(", Success: ").append(execInfo.isSuccess() ? "True" : "False ❌");

		sb.append(LINE_SEPARATOR);

		sb.append("-- Type: ").append(getStatementType(execInfo.getStatementType()));
		sb.append(", QuerySize: ").append(queryInfos.size());
		sb.append(", Batch: ").append(execInfo.isBatch() ? "True" : "False");
		if (execInfo.isBatch()) {
			sb.append(", BatchSize: ").append(execInfo.getBatchSize());
		}
		return sb.toString();
	}

	private String formatParams(ExecutionInfo execInfo, ParameterSetOperation params) {
		if (execInfo.getStatementType() == StatementType.PREPARED) {
			return getParameterValueToDisplay(params);
		} else {
			return getParameterKeyToDisplay(params) + "=" + getParameterValueToDisplay(params);
		}
	}
}
