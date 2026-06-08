package de.cronn.testutils.jpa.query;

import java.util.List;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;

record CapturedQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
}
