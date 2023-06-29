package de.prob2.ui.internal;

import java.nio.file.Path;
import java.util.Locale;

public final class IOHelper {
	private IOHelper() {
	}

	public static String getExtension(Path p) {
		String name = p.getFileName().toString();
		int lastDot = name.lastIndexOf('.');
		// we ignore file names that start or end with a dot
		if (lastDot > 0 && lastDot < name.length() - 1) {
			return name.substring(lastDot + 1).toLowerCase(Locale.ROOT);
		}

		return "";
	}
}
