package de.cronn.testutils;

public class ThreadLeakException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public ThreadLeakException(String message) {
		super(message);
	}
}
