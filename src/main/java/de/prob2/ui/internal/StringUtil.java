package de.prob2.ui.internal;

import java.util.Locale;
import java.util.Objects;

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

		String[] parts = snakeCase.split("_");
		if (parts.length == 1) { // parts.length cannot be 0
			return parts[0].toLowerCase(Locale.ROOT);
		}

		StringBuilder b = new StringBuilder();
		b.append(parts[0].toLowerCase(Locale.ROOT));
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			if (part.isEmpty()) {
				continue;
			}

			b.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
			b.append(part.substring(1).toLowerCase(Locale.ROOT));
		}

		return b.toString();
	}
}
