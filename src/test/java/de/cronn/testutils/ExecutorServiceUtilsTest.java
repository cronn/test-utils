package de.cronn.testutils;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ExecutorServiceUtilsTest {

	@Test
	@Timeout(30)
	void shouldShutDownInfiniteLoopAndClearQueue() {
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		try {
			Callable<Void> task = () -> {
				for (int i = 0; i < 30; i++) {
					Thread.sleep(1000);
				}
				return null;
			};
			Future<Void> firstFuture = executorService.submit(task);
			for (int i = 0; i < 10; i++) {
				executorService.submit(task);
			}
			Future<Void> lastFuture = executorService.submit(task);

			ExecutorServiceUtils.shutdownOrThrow(executorService, "TestExecutorService", 3000);

			assertThat(firstFuture).isDone();
			assertThat(lastFuture).isNotDone();
			assertThat(((ThreadPoolExecutor) executorService).getQueue()).isEmpty();
			assertThat(executorService.isShutdown()).isTrue();
			assertThat(executorService.isTerminated()).isTrue();
		} finally {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
			}
		}
	}
}
