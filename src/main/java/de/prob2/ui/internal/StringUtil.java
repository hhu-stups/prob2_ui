package de.prob2.ui.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class StringUtil {

	private StringUtil() {
	}

	/**
	 * Converts a string from snake_case or SCREAMING_SNAKE_CASE into camelCase.
	 *
	 * @param snakeCase input string in snake_case or SCREAMING_SNAKE_CASE
	 * @return output string in camelCase
	 */
	public static String snakeCaseToCamelCase(String snakeCase) {
		Objects.requireNonNull(snakeCase, "snakeCase");
		if (snakeCase.isEmpty()) {
			return "";
		}

		List<String> parts = Arrays.stream(snakeCase.split("_"))
				                     .filter(part -> !part.isEmpty())
				                     .collect(Collectors.toList());
		if (parts.isEmpty()) {
			return "";
		} else if (parts.size() == 1) {
			return parts.get(0).toLowerCase(Locale.ROOT);
		}

		StringBuilder b = new StringBuilder();
		b.append(parts.get(0).toLowerCase(Locale.ROOT));
		for (int i = 1; i < parts.size(); i++) {
			String part = parts.get(i);
			b.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
			b.append(part.substring(1).toLowerCase(Locale.ROOT));
		}

		return b.toString();
	}
}
