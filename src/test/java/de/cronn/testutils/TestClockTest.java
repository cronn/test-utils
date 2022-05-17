package de.cronn.testutils;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TestClockTest {

	private static final LocalDate BEGIN_OF_2016 = LocalDate.of(2016, Month.JANUARY, 1);
	private static final Instant BEGIN_OF_2016_INSTANT = Instant.parse("2016-01-01T00:00:00.123456Z");

	@Test
	void shouldReset() {
		String future = "2020-12-31T10:10:10.654321Z";
		TestClock testClock = new TestClock(future, "Europe/Berlin");
		assertThat(testClock.instant()).isEqualTo(Instant.parse(future));
		assertThat(testClock.getZone()).isEqualTo(ZoneId.of("Europe/Berlin"));

		testClock.reset();

		assertThat(testClock.instant()).isEqualTo(BEGIN_OF_2016_INSTANT);
		assertThat(testClock.getZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
	}

	@Test
	void shouldWindForwardSeconds() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(testClock.instant()).isEqualTo(BEGIN_OF_2016_INSTANT);

		testClock.windForwardSeconds(42);

		assertThat(testClock.instant()).isEqualTo(Instant.parse("2016-01-01T00:00:42.123456Z"));
	}

	@Test
	void shouldWindForwardHours() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(testClock.instant()).isEqualTo(BEGIN_OF_2016_INSTANT);

		testClock.windForwardHours(7);

		assertThat(testClock.instant()).isEqualTo(Instant.parse("2016-01-01T07:00:00.123456Z"));
	}

	@Test
	void shouldWindForwardToDate() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);

		testClock.windForwardToDate(LocalDate.parse("2016-01-13"));

		assertThat(LocalDate.now(testClock)).isEqualTo(LocalDate.of(2016, Month.JANUARY, 13));
	}

	@Test
	void shouldWindForwardToSameDate() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);

		testClock.windForwardToDate(BEGIN_OF_2016);

		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);
	}

	@Test
	void shouldFailWindForwardToDate() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);

		LocalDate targetDate = LocalDate.parse("2015-12-01");
		assertThatThrownBy(() -> testClock.windForwardToDate(targetDate))
			.isInstanceOf(IllegalArgumentException.class).hasMessage("Cannot wind clock(now=2016-01-01T00:00:00.123456Z) *forward* to date in past(2015-12-01T00:00:00.123456Z)");
	}

	@Test
	void shouldFailWindForwardDaysNegativeDuration() {
		TestClock testClock = TestClock.defaultUtc();

		assertThatThrownBy(() -> testClock.windForwardDays(-1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Cannot wind clock *forward* by a negative duration: PT-24H");
	}

	@Test
	void shouldFailWindForwardNegativeDuration() {
		TestClock testClock = TestClock.defaultUtc();

		Duration duration = Duration.ofSeconds(-10);
		assertThatThrownBy(() -> testClock.windForward(duration))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Cannot wind clock *forward* by a negative duration: PT-10S");
	}

	@Test
	void shouldWindBackSeconds() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(testClock.instant()).isEqualTo(BEGIN_OF_2016_INSTANT);

		testClock.windBackSeconds(42);

		assertThat(testClock.instant()).isEqualTo(Instant.parse("2015-12-31T23:59:18.123456Z"));
	}

	@Test
	void shouldWindBackHours() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(testClock.instant()).isEqualTo(BEGIN_OF_2016_INSTANT);

		testClock.windBackHours(7);

		assertThat(testClock.instant()).isEqualTo(Instant.parse("2015-12-31T17:00:00.123456Z"));
	}

	@Test
	void shouldWindBackToDate() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);

		testClock.windBackToDate(LocalDate.parse("2015-12-01"));

		assertThat(LocalDate.now(testClock)).isEqualTo(LocalDate.of(2015, Month.DECEMBER, 1));
	}

	@Test
	void shouldWindBackToSameDate() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);

		testClock.windBackToDate(BEGIN_OF_2016);

		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);
	}

	@Test
	void shouldFailWindBackToDate() {
		TestClock testClock = TestClock.defaultUtc();
		assertThat(LocalDate.now(testClock)).isEqualTo(BEGIN_OF_2016);

		LocalDate targetDate = LocalDate.parse("2020-01-01");
		assertThatThrownBy(() -> testClock.windBackToDate(targetDate))
			.isInstanceOf(IllegalArgumentException.class).hasMessage("Cannot wind clock(now=2016-01-01T00:00:00.123456Z) *backward* to date in the future(2020-01-01T00:00:00.123456Z)");
	}

	@Test
	void shouldFailWindBackDaysNegativeDuration() {
		TestClock testClock = TestClock.defaultUtc();

		assertThatThrownBy(() -> testClock.windBackDays(-1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Cannot wind clock *backward* by a negative duration: PT-24H");
	}

	@Test
	void shouldFailWindBackNegativeDuration() {
		TestClock testClock = TestClock.defaultUtc();
		Duration duration = Duration.ofSeconds(-10);
		assertThatThrownBy(() -> testClock.windBack(duration))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Cannot wind clock *backward* by a negative duration: PT-10S");
	}
}

