package de.cronn.testutils.jpa.query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

public class QueryCapturingListener implements QueryExecutionListener {

	private final AtomicBoolean listening = new AtomicBoolean();
	private final List<CapturedQuery> capturedQueries = new ArrayList<>();

	@Override
	public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		// noop
	}

	@Override
	public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		if (!listening.get()) {
			return;
		}
		CapturedQuery capturedQuery = new CapturedQuery(execInfo, List.copyOf(queryInfoList));
		synchronized (capturedQueries) {
			capturedQueries.add(capturedQuery);
		}
	}

	void startListening() {
		if (!listening.compareAndSet(false, true)) {
			throw new IllegalStateException("Already listening - nested capturing is not supported");
		}
		synchronized (capturedQueries) {
			capturedQueries.clear();
		}
	}

	void stopListening() {
		if (!listening.compareAndSet(true, false)) {
			throw new IllegalStateException("Not listening");
		}
	}

	List<CapturedQuery> getCapturedQueries() {
		return capturedQueries;
	}
}
