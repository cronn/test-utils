package de.cronn.testutils.postgres.app;

import jakarta.transaction.Transactional;

public class TransactionUtil {

	@Transactional
	public void doInTransaction(Runnable action) {
		action.run();
	}

}
