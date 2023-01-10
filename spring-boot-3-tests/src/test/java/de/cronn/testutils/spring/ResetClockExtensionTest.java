package de.cronn.testutils.spring;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.cronn.testutils.TestClock;

@Import(TestClock.class)
@ExtendWith(SpringExtension.class)
class ResetClockExtensionTest {

	@Autowired
	private TestClock clock;

	@TestMethodOrder(MethodOrderer.MethodName.class)
	@ExtendWith(ResetClockExtension.class)
	@Nested
	class DefinedTestMethodOrder {

		@Test
		void a_assertDefaultTime() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT);
		}

		@Test
		void b_windClockForward() {
			clock.windForwardHours(3L);
		}

		@Test
		void c_assertNotReset() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT.plus(3, ChronoUnit.HOURS));
		}
	}

	@ExtendWith(ResetClockExtension.class)
	@Nested
	class NoTestMethodOrder {

		@Test
		void windAndAssert1() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT);
			clock.windForwardHours(3L);
		}

		@Test
		void windAndAssert2() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT);
			clock.windForwardHours(3L);
		}

	}

	@TestMethodOrder(MethodOrderer.MethodName.class)
	@Nested
	class CustomResetCondition {

		@RegisterExtension
		private ResetClockExtension resetClockExtension = new ResetClockExtension(
			extensionContext -> extensionContext.getTestMethod()
				.map(method -> method.getName().endsWith("ResetClock"))
				.orElse(false)
		);

		@Test
		void a_assertDefaultTime() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT);
		}

		@Test
		void b_windClockForward() {
			clock.windForwardHours(3L);
		}

		@Test
		void c_assertNotReset() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT.plus(3, ChronoUnit.HOURS));
		}

		@Test
		void d_windClockForwardAndResetClock() {
			clock.windForwardHours(3L);
		}

		@Test
		void e_assertReset() {
			assertClockInstance(TestClock.DEFAULT_TEST_INSTANT);
		}

	}

	private void assertClockInstance(Instant defaultTestInstant) {
		Assertions.assertThat(clock).returns(defaultTestInstant, TestClock::instant);
	}
}
