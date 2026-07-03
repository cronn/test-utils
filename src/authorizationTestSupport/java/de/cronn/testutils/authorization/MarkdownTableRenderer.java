package de.cronn.testutils.authorization;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Default {@link ResultsRenderer} that renders the authorization matrix as a Markdown table
 * with the columns METHOD, PATH and ALLOWED_ROLES, padded to uniform column widths.
 *
 * <p>Designed for extension: subclasses may override the {@code protected} methods to customize
 * individual aspects of the rendering, e.g. {@link #formatAllowedCell(EndpointResult, Set)} for
 * the cell content or {@link #separatorRow(int[])} for the separator style.
 */
public class MarkdownTableRenderer implements ResultsRenderer {

	protected static final List<String> TABLE_HEADER = List.of("METHOD", "PATH", "ALLOWED_ROLES");

	@Override
	public String render(List<EndpointResult> results, Collection<? extends Credentials> credentials) {
		Set<String> allCredentialNames = allNames(credentials);
		List<List<String>> rows = results.stream()
			.map(result -> List.of(
				result.endpoint().method().name(),
				result.endpoint().path(),
				formatAllowedCell(result, allCredentialNames)))
			.toList();
		int[] widths = columnWidths(rows);
		StringBuilder sb = new StringBuilder();
		sb.append(tableRow(TABLE_HEADER, widths));
		sb.append(separatorRow(widths));
		rows.forEach(row -> sb.append(tableRow(row, widths)));
		return sb.toString();
	}

	protected int[] columnWidths(List<List<String>> rows) {
		int[] widths = TABLE_HEADER.stream().mapToInt(String::length).toArray();
		for (List<String> row : rows) {
			for (int i = 0; i < widths.length; i++) {
				widths[i] = Math.max(widths[i], row.get(i).length());
			}
		}
		return widths;
	}

	protected String tableRow(List<String> cells, int[] widths) {
		StringBuilder row = new StringBuilder("|");
		for (int i = 0; i < cells.size(); i++) {
			row.append(' ').append(padRight(cells.get(i), widths[i])).append(" |");
		}
		return row.append('\n').toString();
	}

	protected String separatorRow(int[] widths) {
		return IntStream.of(widths)
			.mapToObj("-"::repeat)
			.collect(Collectors.joining("-|-", "|-", "-|\n"));
	}

	protected String padRight(String value, int width) {
		return value + " ".repeat(width - value.length());
	}

	protected String formatAllowedCell(EndpointResult result, Set<String> allCredentialNames) {
		if (result.unauthenticatedAllowed()) {
			return "{⚠ PERMIT_ALL ⚠}";
		}
		if (Boolean.TRUE.equals(result.authenticatedAllowed())) {
			return "{AUTHENTICATED}";
		}
		Set<String> allowed = new LinkedHashSet<>(result.allowedRoles());
		if (allowed.equals(allCredentialNames)) {
			return "{ANY_ROLE}";
		} else {
			return String.join("<br>", allowed);
		}
	}

	protected Set<String> allNames(Collection<? extends Credentials> credentials) {
		return credentials.stream()
			.map(Credentials::name)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
