package de.cronn.testutils.hibernate.app;

import java.util.concurrent.Callable;

import jakarta.transaction.Transactional;

public class TransactionUtil {

	@Transactional
	public void doInTransaction(Runnable action) {
		action.run();
	}

	@Transactional
	public <T> T doInTransactionWithResult(Callable<T> action) {
		try {
			return action.call();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
