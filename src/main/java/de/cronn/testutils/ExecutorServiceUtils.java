package de.cronn.testutils;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecutorServiceUtils {

	private static final Logger log = LoggerFactory.getLogger(ExecutorServiceUtils.class);

	private ExecutorServiceUtils() {
	}

	public static void shutdownOrThrow(ExecutorService executor, String executorServiceName, long timeoutMillis) {
		shutdownOrThrow(executor, executorServiceName, Duration.ofMillis(timeoutMillis));
	}

	public static void shutdownOrThrow(ExecutorService executor, String executorServiceName, Duration timeout) {
		if (executor != null) {
			try {
				if (!shutdownGracefully(executor, executorServiceName, timeout)) {
					boolean success = shutdownNow(executor, executorServiceName, timeout);
					Assertions.assertTrue(success, String.format("Failed to shutdown %s", executorServiceName));
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new TestUtilsRuntimeException("Failed to shutdown " + executorServiceName);
			}
		}
	}

	public static boolean shutdownNow(ExecutorService executorService, String executorServiceName, long timeoutMillis) throws InterruptedException {
		return shutdownNow(executorService, executorServiceName, Duration.ofMillis(timeoutMillis));
	}

	public static boolean shutdownNow(ExecutorService executorService, String executorServiceName, Duration timeout) throws InterruptedException {
		return shutdown(executorService, executorServiceName, timeout, true);
	}

	public static boolean shutdownGracefully(ExecutorService executorService, String executorServiceName, long timeoutMillis) throws InterruptedException {
		return shutdownGracefully(executorService, executorServiceName, Duration.ofMillis(timeoutMillis));
	}

	public static boolean shutdownGracefully(ExecutorService executorService, String executorServiceName, Duration timeout) throws InterruptedException {
		return shutdown(executorService, executorServiceName, timeout, false);
	}

	private static boolean shutdown(ExecutorService executorService, String executorServiceName, Duration timeout, boolean shutdownWithInterrupt) throws InterruptedException {
		return shutdown(executorService, executorServiceName, timeout.toMillis(), shutdownWithInterrupt);
	}

	private static boolean shutdown(ExecutorService executorService, String executorServiceName, long timeoutMillis, boolean shutdownWithInterrupt) throws InterruptedException {
		log.debug("Shutting down {}", executorServiceName);

		if (shutdownWithInterrupt) {
			executorService.shutdownNow();
		} else {
			executorService.shutdown();
		}

		clearQueue(executorService, executorServiceName);

		boolean success = executorService.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
		if (success) {
			log.info("Finished shutdown of '{}'", executorServiceName);
		} else {
			if (executorService instanceof ThreadPoolExecutor threadPoolExecutor) {
				log.warn("Shutdown of '{}' timed out after {} ms. Active tasks: {}", executorServiceName, timeoutMillis, threadPoolExecutor.getActiveCount());
			} else {
				log.warn("Shutdown of '{}' timed out after {} ms.", executorServiceName, timeoutMillis);
			}
		}
		return success;
	}

	private static void clearQueue(ExecutorService executorService, String executorServiceName) {
		if (executorService instanceof ThreadPoolExecutor threadPoolExecutor) {
			BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
			if (!queue.isEmpty()) {
				int queueSize = queue.size();
				log.warn("Clearing approximately {} elements from queue of '{}'", queueSize, executorServiceName);
				queue.clear();
			}
		}
	}
}
