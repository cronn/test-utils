package de.cronn.testutils.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

@FunctionalInterface
public interface EventFilter {

  boolean shouldCapture(ILoggingEvent loggingEvent);

  static EventFilter all() {
    return event -> true;
  }

  default EventFilter and(EventFilter otherEventFilter) {
    return event -> shouldCapture(event) && otherEventFilter.shouldCapture(event);
  }

  default EventFilter or(EventFilter otherEventFilter) {
    return event -> shouldCapture(event) || otherEventFilter.shouldCapture(event);
  }

  static EventFilter forClass(Class<?> clazz) {
    return event -> clazz.getName().equals(event.getLoggerName());
  }

  static EventFilter atLeastWarning() {
    return levelGreaterOrEqual(Level.WARN);
  }

  static EventFilter levelGreaterOrEqual(Level minLevel) {
    return event -> event.getLevel().isGreaterOrEqual(minLevel);
  }
}
