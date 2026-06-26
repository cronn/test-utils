package de.cronn.testutils;

import java.io.Serial;

public class TestUtilsRuntimeException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1L;

	public TestUtilsRuntimeException() {
		super();
	}

	public TestUtilsRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TestUtilsRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
	public TestUtilsRuntimeException(String message) {
		super(message);
	}

	public TestUtilsRuntimeException(Throwable cause) {
		super(cause);
	}
}
