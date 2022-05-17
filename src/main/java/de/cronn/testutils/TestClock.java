package de.cronn.testutils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestClock extends Clock {

	public static final Instant DEFAULT_TEST_INSTANT = Instant.parse("2016-01-01T00:00:00.123456Z");

	private static final Logger log = LoggerFactory.getLogger(TestClock.class);

	private final AtomicReference<Instant> instant;
	private final ZoneId zone;

	public TestClock() {
		this(DEFAULT_TEST_INSTANT, ZoneOffset.UTC);
	}

	public TestClock(Instant instant, ZoneId zone) {
		this.instant = new AtomicReference<>(instant);
		this.zone = zone;
	}

	public TestClock(String instant, String zone) {
		this(Instant.parse(instant), ZoneId.of(zone));
	}

	@Override
	public Instant instant() {
		return instant.get();
	}

	public ZonedDateTime now() {
		return ZonedDateTime.ofInstant(instant(), zone);
	}

	public LocalDate today() {
		return LocalDate.now(this);
	}

	public void changeInstant(Instant instant) {
		this.instant.set(instant);
		log.info("Setting test time to {}", instant);
	}

	public void reset() {
		changeInstant(DEFAULT_TEST_INSTANT);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		Objects.requireNonNull(zone);
		if (zone.equals(this.zone)) {
			return this;
		}
		return new TestClock(instant.get(), this.zone);
	}

	public Instant windForward(Duration duration) {
		if (duration.isNegative()) {
			throw new IllegalArgumentException(String.format("Cannot wind clock *forward* by a negative duration: %s", duration));
		}
		Instant newInstant = instant.updateAndGet(i -> i.plus(duration));
		log.info("Winding test time forward to {}", newInstant);
		return newInstant;
	}

	public void windForwardSeconds(int seconds) {
		windForward(Duration.ofSeconds(seconds));
	}

	public void windForwardHours(long hours) {
		windForward(Duration.ofHours(hours));
	}

	public void windForwardDays(long days) {
		windForward(Duration.ofDays(days));
	}

	public void windForwardToDate(LocalDate targetDate) {
		LocalDate oldDate = today();
		long daysToWindForward = ChronoUnit.DAYS.between(oldDate, targetDate);
		if (daysToWindForward < 0) {
			Instant targetInstant = instant().plus(Duration.ofDays(daysToWindForward));
			throw new IllegalArgumentException(String.format("Cannot wind clock(now=%s) *forward* to date in past(%s)", instant(), targetInstant));
		} else if (daysToWindForward > 0) {
			windForwardDays(daysToWindForward);
		} else {
			Assertions.assertEquals(targetDate, oldDate, String.format("%s must be equal to %s", targetDate, oldDate));
			log.info("Keeping test time at {}", instant());
		}
	}

	public void windBack(Duration duration) {
		if (duration.isNegative()) {
			throw new IllegalArgumentException(String.format("Cannot wind clock *backward* by a negative duration: %s", duration));
		}
		Instant newInstant = instant.updateAndGet(i -> i.minus(duration));
		log.info("Winding test time back to {}", newInstant);
	}

	public void windBackSeconds(long seconds) {
		windBack(Duration.ofSeconds(seconds));
	}

	public void windBackHours(long hours) {
		windBack(Duration.ofHours(hours));
	}

	public void windBackDays(long days) {
		windBack(Duration.ofDays(days));
	}

	public void windBackToDate(LocalDate targetDate) {
		LocalDate oldDate = today();
		long daysToWindBackward = ChronoUnit.DAYS.between(targetDate, oldDate);
		if (daysToWindBackward < 0) {
			Instant targetInstant = instant().minus(Duration.ofDays(daysToWindBackward));
			throw new IllegalArgumentException(String.format("Cannot wind clock(now=%s) *backward* to date in the future(%s)", instant(), targetInstant));
		} else if (daysToWindBackward > 0) {
			windBackDays(daysToWindBackward);
		} else {
			Assertions.assertEquals(targetDate, oldDate, String.format("%s must be equal to %s", targetDate, oldDate));
			log.info("Keeping test time at {}", instant());
		}
	}

	public static TestClock defaultUtc() {
		return new TestClock(DEFAULT_TEST_INSTANT, ZoneOffset.UTC);
	}

}

