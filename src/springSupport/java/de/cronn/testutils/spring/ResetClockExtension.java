package de.cronn.testutils.spring;

import java.time.Clock;
import java.util.function.Predicate;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.cronn.testutils.TestClock;

public class ResetClockExtension implements AfterAllCallback, AfterEachCallback {

	private final Predicate<ExtensionContext> shouldResetAfterEach;

	public ResetClockExtension(Predicate<ExtensionContext> shouldResetAfterEach) {
		this.shouldResetAfterEach = shouldResetAfterEach;
	}

	public ResetClockExtension() {
		this(context -> !hasDeclaredMethodOrder(context));
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (shouldResetAfterEach.test(context)) {
			resetClock(context);
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		resetClock(context);
	}

	public static boolean hasDeclaredMethodOrder(ExtensionContext context) {
		TestMethodOrder annotation = context.getRequiredTestClass().getAnnotation(TestMethodOrder.class);
		return annotation != null && annotation.value() != MethodOrderer.Random.class;
	}

	protected void resetClock(ExtensionContext context) {
		ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
		applicationContext.getBeanProvider(Clock.class).ifAvailable(clock -> {
			if (clock instanceof TestClock testClock) {
				testClock.reset();
			}
		});
	}

}
