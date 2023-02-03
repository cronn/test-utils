package de.cronn.testutils.h2.app;

import javax.transaction.Transactional;

public class TransactionUtil {

	public interface Action {
		void execute();
	}

	@Transactional
	public void doInTransaction(Action action) {
		action.execute();
	}
}
