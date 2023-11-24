package de.cronn.testutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.ThreadUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ThreadLeakCheck implements BeforeAllCallback, AfterAllCallback {

	private static final Logger log = LoggerFactory.getLogger(ThreadLeakCheck.class);

	private static final Duration THREAD_SHUTDOWN_GRACE_PERIOD = Duration.ofMillis(500);

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ThreadLeakCheck.class);

	@Override
	public void beforeAll(ExtensionContext context) {
		if (store(context).get(Keys.EXTENDED_TEST_CLASS) == null) {
			if (context.getParent().orElse(null) != context.getRoot()) {
				throw new IllegalStateException("Extension has to be registered at top class level");
			}
			store(context).put(Keys.EXTENDED_TEST_CLASS, context.getRequiredTestClass());
			store(context).put(Keys.THREADS_BEFORE_TEST, getAllLivingThreadNamesById());
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		Class<?> extendedClass = (Class<?>) store(context).get(Keys.EXTENDED_TEST_CLASS);
		if (context.getRequiredTestClass().equals(extendedClass)) {

			Set<String> allowedThreadNames = new LinkedHashSet<>();
			Set<String> allowedThreadNamePrefixes = new LinkedHashSet<>();

			for (Class<?> clazz = extendedClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
				AllowedThreads allowedThreads = clazz.getAnnotation(AllowedThreads.class);
				if (allowedThreads != null) {
					allowedThreadNames.addAll(Arrays.asList(allowedThreads.names()));
					allowedThreadNamePrefixes.addAll(Arrays.asList(allowedThreads.prefixes()));
				}
			}

			@SuppressWarnings("unchecked")
			Map<Long, Thread> threadsBeforeTest = (Map<Long, Thread>) store(context).get(Keys.THREADS_BEFORE_TEST);
			Map<Long, Thread> threadsAfterTest = getAllLivingThreadNamesById();
			threadsAfterTest.keySet().removeAll(threadsBeforeTest.keySet());
			threadsAfterTest.values().removeIf(thread -> allowedThreadNames.contains(thread.getName()));
			threadsAfterTest.values().removeIf(threadNameStartsWithAny(allowedThreadNamePrefixes));
			threadsAfterTest.values().removeIf(this::checkIfThreadTerminatesAfterGracePeriod);
			threadsAfterTest.values().removeIf(this::isAddressChangeListenerThread);
			threadsAfterTest.values().removeIf(this::isIocpEventHandlerTask);

			if (!threadsAfterTest.isEmpty()) {
				throw new ThreadLeakException("Potential thread leak detected. Running threads after test that did not exist before: " +
					threadsAfterTest.values().stream()
						.map(thread -> "'" + thread.getName() + "' (state: " + thread.getState() + ", interrupted: " + thread.isInterrupted() + ")")
						.collect(Collectors.joining(", ")));
			}
		}
	}

	private boolean checkIfThreadTerminatesAfterGracePeriod(Thread thread) {
		log.warn("Giving {} in state {} {} to shut down", thread, thread.getState(), THREAD_SHUTDOWN_GRACE_PERIOD);
		try {
			thread.join(THREAD_SHUTDOWN_GRACE_PERIOD.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Interrupted: ", e);
			return false;
		}
		if (thread.isAlive()) {
			log.error("{} is still alive! Stack trace: \n\t\t{}", thread,
				Arrays.stream(thread.getStackTrace())
					.map(StackTraceElement::toString)
					.collect(Collectors.joining("\n\t\t")));
			return false;
		} else {
			log.info("{} finished", thread);
			return true;
		}
	}

	// Workaround for https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8262929
	private boolean isAddressChangeListenerThread(Thread thread) {
		if (SystemUtils.IS_OS_WINDOWS && thread.getName().startsWith("Thread-")) {
			// See https://github.com/openjdk/jdk/blob/3b350ad87f182c2800ba17458911de877ae24a6d/src/java.base/windows/classes/sun/net/dns/ResolverConfigurationImpl.java#L198
			// Note: It was fixed in JDK 19 via https://github.com/openjdk/jdk/commit/81d7eafd913d28e0c83ddb29f9436b207da5f21c
			return stackTraceContainsClassName(thread, "sun.net.dns.ResolverConfigurationImpl$AddressChangeListener");
		} else {
			return false;
		}
	}

	private boolean isIocpEventHandlerTask(Thread thread) {
		if (SystemUtils.IS_OS_WINDOWS && thread.getName().startsWith("Thread-")) {
			// The JDK on Windows starts unnamed threads in https://github.com/openjdk/jdk/blob/0deb648985b018653ccdaf193dc13b3cf21c088a/src/java.base/windows/classes/sun/nio/ch/Iocp.java#L75
			return stackTraceContainsClassName(thread, "sun.nio.ch.Iocp$EventHandlerTask");
		} else {
			return false;
		}
	}

	private static boolean stackTraceContainsClassName(Thread thread, String className) {
		return Arrays.stream(thread.getStackTrace())
			.anyMatch(stackTraceElement -> className.equals(stackTraceElement.getClassName()));
	}

	private static Map<Long, Thread> getAllLivingThreadNamesById() {
		return ThreadUtils.getAllThreads()
			.stream()
			.filter(Objects::nonNull)
			.filter(Thread::isAlive)
			.sorted(Comparator.comparingLong(Thread::getId))
			.collect(Collectors.toMap(Thread::getId, thread -> thread, (t, t2) -> { throw new IllegalStateException(); }, LinkedHashMap::new ));
	}

	private Predicate<Thread> threadNameStartsWithAny(Collection<String> prefixes) {
		return thread -> {
			for (String prefix : prefixes) {
				if (thread.getName().startsWith(prefix)) {
					return true;
				}
			}
			return false;
		};
	}

	private ExtensionContext.Store store(ExtensionContext context) {
		return context.getStore(NAMESPACE);
	}

	enum Keys {
		THREADS_BEFORE_TEST,
		EXTENDED_TEST_CLASS,
		;
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AllowedThreads {

		String[] prefixes() default {};

		String[] names() default {};
	}
}
