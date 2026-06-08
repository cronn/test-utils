package de.cronn.testutils.jpa.query;

import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import de.cronn.commons.lang.Action;
import net.ttddyy.dsproxy.listener.logging.QueryLogEntryCreator;

@Component
public class QueryCaptor {

	private final QueryCapturingListener queryCapturingListener;
	private final QueryLogEntryCreator queryLogEntryCreator;

	public QueryCaptor(QueryCapturingListener queryCapturingListener, QueryLogEntryCreator queryLogEntryCreator) {
		this.queryCapturingListener = queryCapturingListener;
		this.queryLogEntryCreator = queryLogEntryCreator;
	}

	public void captureQueriesDuring(Action action) throws Exception {
		captureQueriesDuring(action.toCallable());
	}

	public <T> T captureQueriesDuring(Callable<T> callable) throws Exception {
		this.queryCapturingListener.startListening();
		try {
			return callable.call();
		} finally {
			this.queryCapturingListener.stopListening();
		}
	}

	public List<String> getCapturedQueries() {
		List<CapturedQuery> capturedQueries = queryCapturingListener.getCapturedQueries();
		return capturedQueries.stream()
			.map(capturedQuery -> queryLogEntryCreator.getLogEntry(capturedQuery.executionInfo(), capturedQuery.queryInfoList(), true, false, true).trim()).toList();
	}
}
