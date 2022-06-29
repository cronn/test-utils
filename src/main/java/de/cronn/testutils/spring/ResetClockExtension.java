package de.cronn.testutils.spring;

import java.time.Clock;
import java.util.function.Function;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.cronn.testutils.TestClock;

public class ResetClockExtension implements AfterAllCallback, AfterEachCallback {

	private final Function<ExtensionContext, Boolean> resetAfter;

	public ResetClockExtension(Function<ExtensionContext, Boolean> resetAfter) {
		this.resetAfter = resetAfter;
	}

	public ResetClockExtension() {
		this(context -> !hasDeclaredMethodOrder(context));
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (resetAfter.apply(context)) {
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
		Clock clock = applicationContext.getBean(Clock.class);
		if (clock instanceof TestClock) {
			((TestClock) clock).reset();
		}
	}

}
