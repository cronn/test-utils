package de.cronn.testutils.jpa.query;

@FunctionalInterface
public interface SqlQueryFormatter {
	String format(String query);
}
