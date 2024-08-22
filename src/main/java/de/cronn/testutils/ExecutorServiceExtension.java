package de.cronn.testutils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

public class ExecutorServiceExtension implements BeforeEachCallback, AfterEachCallback {

	private final Duration testTimeout;
	private ExecutorService executorService;
	private List<Future<?>> futures;

	public ExecutorServiceExtension(long testTimeoutMillis) {
		this(Duration.ofMillis(testTimeoutMillis));
	}

	public ExecutorServiceExtension(Duration testTimeout) {
		this.testTimeout = testTimeout;
	}

	@Override
	public void afterEach(ExtensionContext context) {
		ExecutorServiceUtils.shutdownOrThrow(executorService, getTestName(context), testTimeout);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		ThreadFactory threadFactory = new CustomizableThreadFactory(getTestName(context));
		executorService = Executors.newCachedThreadPool(threadFactory);
		futures = new ArrayList<>();
	}

	private String getTestName(ExtensionContext context) {
		return TestNameUtils.getTestName(context.getRequiredTestClass(), context.getRequiredTestMethod().getName());
	}

	public Future<Void> submit(Runnable runnable) {
		return submit(() -> {
			runnable.run();
			return null;
		});
	}

	public <T> Future<T> submit(Callable<T> callable) {
		Future<T> future = executorService.submit(callable);
		futures.add(future);
		return future;
	}

	public List<Future<?>> getFutures() {
		return futures;
	}

	public void awaitAllFutures() throws Exception {
		for (Future<?> future : getFutures()) {
			future.get();
		}
	}

	static class TestNameUtils {

		private TestNameUtils() {
		}

		public static String getTestName(Class<?> aClass, String methodName) {
			return join(enclosingClassesUpstream(aClass), methodName);
		}

		private static String enclosingClassesUpstream(Class<?> aClass) {
			String classHierarchy = aClass.getSimpleName();
			Class<?> enclosingClass = aClass.getEnclosingClass();
			while (enclosingClass != null) {
				classHierarchy = join(enclosingClass.getSimpleName(), classHierarchy);
				enclosingClass = enclosingClass.getEnclosingClass();
			}
			return classHierarchy;
		}

		private static String join(String element, String other) {
			return other.startsWith("_") ? (element + other) : (element + "_" + other);
		}
	}

}
