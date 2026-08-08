package de.cronn.testutils.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

record RenderingLoggingEvent(ILoggingEvent event, RenderingOptions options) {

  private static final String INFIX = " ";

  String getPrefix() {
    String prefix = "";

    if (options.includeLoggerName()) {
      prefix +=
          ("[%" + options.loggerNameLength() + "s]")
              .formatted(right(event.getLoggerName(), options.loggerNameLength()));
    }
    if (options.includeLogLevel()) {
      prefix += " [%-5s]".formatted(event.getLevel());
    }
    if (!event.getMDCPropertyMap().isEmpty()) {
      prefix += " " + event.getMDCPropertyMap();
    }
    return prefix;
  }

  int getPrefixLength() {
    return getPrefix().length();
  }

  private String getFormattedMessage(int padPrefix) {
    String prefixPadding = " ".repeat(padPrefix + INFIX.length());
    return event().getFormattedMessage().replace("\n", "\n" + prefixPadding).trim()
        + getException(prefixPadding.length());
  }

  private String getException(int prefixPaddingLength) {
    IThrowableProxy throwable = event().getThrowableProxy();
    if (throwable == null) {
      return "";
    }
    return formatThrowable(prefixPaddingLength, "", throwable);
  }

  private static String formatThrowable(
      int prefixPaddingLength, String prefix, IThrowableProxy throwable) {
    String exceptionName =
        right(throwable.getClassName() + ": ", prefixPaddingLength - prefix.length());
    String message =
        "\n"
            + ("%" + prefixPaddingLength + "s").formatted(prefix + exceptionName)
            + throwable.getMessage();
    if (throwable.getCause() != null) {
      return message + formatThrowable(prefixPaddingLength, "Caused by: ", throwable.getCause());
    }
    return message;
  }

  String render(int padPrefix) {
    String prefix = getPrefix();
    String paddedPrefix = prefix + " ".repeat(Math.max(0, padPrefix - prefix.length()));
    return paddedPrefix + INFIX + getFormattedMessage(padPrefix);
  }

  private static String right(String string, int length) {
    if (length <= 0) {
      return "";
    }
    if (string.length() <= length) {
      return string;
    }
    return string.substring(string.length() - length);
  }
}
