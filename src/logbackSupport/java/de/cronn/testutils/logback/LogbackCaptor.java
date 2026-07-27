package de.cronn.testutils.logback;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.cronn.commons.lang.Action;

public class LogbackCaptor {

	private final String loggerName;
	private final RenderingOptions renderingOptions;
	private final EventFilter filter;
	private final Level level;

	private CapturingAppender lastAppender;

	public LogbackCaptor(String loggerName, RenderingOptions renderingOptions, EventFilter filter, Level level) {
		this.loggerName = loggerName;
		this.renderingOptions = renderingOptions;
		this.filter = filter;
		this.level = level;
	}

	public void captureLoggingDuring(Action action) throws Exception {
		captureLoggingDuring(action.toCallable());
	}

	public <T> T captureLoggingDuring(Callable<T> callable) throws Exception {
		try (CapturingAppender appender = CapturingAppender.create(loggerName, renderingOptions, filter, level)) {
			this.lastAppender = appender;
			return callable.call();
		}
	}

	public Stream<ILoggingEvent> getCapturedLoggingEvents() {
		return Objects.requireNonNull(lastAppender).getLoggingEvents();
	}

	public Stream<String> getCapturedLog() {
		return Objects.requireNonNull(lastAppender).renderEvents();
	}
}
