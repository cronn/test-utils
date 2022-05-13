package de.cronn.testutils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ConcurrentTest<T> {

	private static final int TIMEOUT_MILLIS = 30_000;
	private int concurrencyLevel = 10;
	private final IndexedCallable<T> task;
	private String threadNamePrefix = ConcurrentTest.class.getSimpleName();

	public ConcurrentTest(IndexedCallable<T> task) {
		this.task = task;
	}

	public static <T> ConcurrentTest<T> create(IndexedCallable<T> task) {
		return new ConcurrentTest<>(task);
	}

	public ConcurrentTest<T> withConcurrencyLevel(int concurrencyLevel) {
		this.concurrencyLevel = concurrencyLevel;
		return this;
	}

	public ConcurrentTest<T> withThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
		return this;
	}

	public ConcurrentTest<T> withThreadNamePrefixFromClass(Class<?> clazz) {
		return withThreadNamePrefix(clazz.getSimpleName());
	}

	public void runAndAssertEachResult(Consumer<T> assertion) throws InterruptedException, ExecutionException {
		ThreadFactory threadFactory = new NamedThreadFactory(threadNamePrefix);
		ExecutorService executorService = Executors.newFixedThreadPool(concurrencyLevel, threadFactory);
		try {
			CompletionService<T> completionService = new ExecutorCompletionService<>(executorService);

			for (int i = 0; i < concurrencyLevel; i++) {
				completionService.submit(task.toCallable(i));
			}

			for (int i = 0; i < concurrencyLevel; i++) {
				T result = completionService.take().get();
				assertion.accept(result);
			}
		} finally {
			ExecutorServiceUtils.shutdownOrThrow(executorService, threadNamePrefix, TIMEOUT_MILLIS);
		}
	}

	private static class NamedThreadFactory implements ThreadFactory {
		private final String prefix;

		private final AtomicInteger threadCount = new AtomicInteger();

		public NamedThreadFactory(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, prefix + threadCount.incrementAndGet());
		}
	}

	@FunctionalInterface
	public interface IndexedCallable<T> {

		T call(int index) throws InterruptedException;

		default Callable<T> toCallable(int index) {
			return () -> call(index);
		}


	}
}
