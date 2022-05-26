package de.cronn.testutils;

public class TestUtilsRuntimeException extends RuntimeException {

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
