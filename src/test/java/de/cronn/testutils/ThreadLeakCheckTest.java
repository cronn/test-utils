package de.cronn.testutils;

import static de.cronn.testutils.AllowedThreadTest.*;
import static de.cronn.testutils.ThreadLeakCheckTest.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.ThreadUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ThreadLeakCheckTest {

	private static final Logger log = LoggerFactory.getLogger(ThreadLeakCheckTest.class);

	static final String FLAG = "ThreadLeakCheckTestTestFlag";
	static final String FLAG_ENABLED_VALUE = "true";

	static final String LEAKING_THREAD_NAME = "LeakingThreadName";

	@BeforeEach
	void enableTestCaseTestClasses() {
		System.setProperty(FLAG, FLAG_ENABLED_VALUE);
	}

	@AfterEach
	void disableTestCaseTestClasses() {
		System.clearProperty(FLAG);
	}

	@Test
	void testDetectThreadLeak_whitelistCanBeOverridden() throws Exception {
		try {
			List<Throwable> exceptions = JUnitTestExecutor.runTestClassAndReturnExceptionsThrown(AllowedThreadTest.class);
			exceptions.forEach(Throwable::printStackTrace);
			assertThat(exceptions).isEmpty();
		} finally {
			joinThreads(thread -> thread.getName().startsWith(AllowedThreadTest.ALLOWED_THREAD_NAME));
		}
	}

	@Test
	void testDetectThreadLeak() throws Exception {
		try {
			List<Throwable> exceptions = JUnitTestExecutor.runTestClassAndReturnExceptionsThrown(ThreadLeakingTest.class);
			assertThat(exceptions)
				.hasOnlyOneElementSatisfying(
					e ->
						assertThat(e)
							.isInstanceOf(ThreadLeakException.class)
							.hasMessage("Potential thread leak detected. Running threads after test that did not exist before: 'LeakingThreadName' (state: TIMED_WAITING, interrupted: false)")
				);
		} finally {
			joinThreads(thread -> thread.getName().equals(LEAKING_THREAD_NAME));
		}
	}

	@Test
	void testDetectThreadLeakInNestedClass() throws Exception {
		try {
			List<Throwable> exceptions = JUnitTestExecutor.runTestClassAndReturnExceptionsThrown(NestedClassesTest.class);
			assertThat(exceptions)
				.hasOnlyOneElementSatisfying(
					e ->
						assertThat(e)
							.isInstanceOf(ThreadLeakException.class)
							.hasMessage("Potential thread leak detected. Running threads after test that did not exist before: 'LeakingThreadName' (state: TIMED_WAITING, interrupted: false)")
				);
		} finally {
			joinThreads(thread -> thread.getName().equals(LEAKING_THREAD_NAME));
		}
	}

	@Test
	void testInvalidUsageInNestedClass() throws Exception {
		List<Throwable> exceptions = JUnitTestExecutor.runTestClassAndReturnExceptionsThrown(InvalidUsageNestedClassesTest.class);
		assertThat(exceptions)
			.hasOnlyOneElementSatisfying(
				e ->
					assertThat(e)
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Extension has to be registered at top class level")
			);
	}

	private void joinThreads(Predicate<Thread> criteria) throws InterruptedException {
		for (Thread thread : ThreadUtils.getAllThreads()) {
			if (thread != null && criteria.test(thread)) {
				thread.interrupt();
				thread.join(10_000L);
			}
		}
	}

	static Thread startDummyDaemonThread(String threadName) {
		Thread thread = new Thread(() -> {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.info("interrupted", e);
					Thread.currentThread().interrupt();
					break;
				}
			}
		}, threadName);

		thread.setDaemon(true);
		thread.start();

		return thread;
	}

}

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = ThreadLeakCheckTest.FLAG, matches = ThreadLeakCheckTest.FLAG_ENABLED_VALUE)
@org.junit.jupiter.api.extension.ExtendWith(ThreadLeakCheck.class)
class ThreadLeakingTest {

	@Test
	void threadLeakingTest() {
		ThreadLeakCheckTest.startDummyDaemonThread(LEAKING_THREAD_NAME);
	}
}

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = ThreadLeakCheckTest.FLAG, matches = ThreadLeakCheckTest.FLAG_ENABLED_VALUE)
@org.junit.jupiter.api.extension.ExtendWith(ThreadLeakCheck.class)
@ThreadLeakCheck.AllowedThreads(
	names = ALLOWED_THREAD_NAME,
	prefixes = ALLOWED_THREAD_NAME + "-"
)
class AllowedThreadTest {

	static final String ALLOWED_THREAD_NAME = "MySpecialThread";

	@Test
	void testDetectThreadLeak_whitelistCanBeOverridden() throws Exception {
		ThreadLeakCheckTest.startDummyDaemonThread(ALLOWED_THREAD_NAME);
		ThreadLeakCheckTest.startDummyDaemonThread(ALLOWED_THREAD_NAME + "-1");
		ThreadLeakCheckTest.startDummyDaemonThread(ALLOWED_THREAD_NAME + "-2");
	}

}

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = ThreadLeakCheckTest.FLAG, matches = ThreadLeakCheckTest.FLAG_ENABLED_VALUE)
@org.junit.jupiter.api.extension.ExtendWith(ThreadLeakCheck.class)
class NestedClassesTest {

	@org.junit.jupiter.api.Nested
	class Nested {

		@Test
		void threadLeakingTest() {
			ThreadLeakCheckTest.startDummyDaemonThread(LEAKING_THREAD_NAME);
		}
	}
}

@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = ThreadLeakCheckTest.FLAG, matches = ThreadLeakCheckTest.FLAG_ENABLED_VALUE)
class InvalidUsageNestedClassesTest {

	@org.junit.jupiter.api.extension.ExtendWith(ThreadLeakCheck.class)
	@org.junit.jupiter.api.Nested
	class Nested {

		@Test
		void test() {
		}
	}
}
