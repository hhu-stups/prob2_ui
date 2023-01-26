package de.prob2.ui.internal;

public final class StringHelper {
	private StringHelper() {
	}

	public static String escapeNonAscii(String s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (' ' <= c && c <= '~') {
				b.append(c);
			} else {
				String esc;
				switch (c) {
					case '\t':
						esc = "\\t";
						break;
					case '\b':
						esc = "\\b";
						break;
					case '\n':
						esc = "\\n";
						break;
					case '\r':
						esc = "\\r";
						break;
					case '\f':
						esc = "\\f";
						break;
					default:
						esc = "\\u{" + Integer.toHexString(c) + "}";
				}

				b.append(esc);
			}
		}
		return b.toString();
	}

	public static boolean containsNoControlCharacters(String s) {
		if (s.isEmpty()) {
			return true;
		} else if (s.length() == 1) {
			return !Character.isISOControl(s.charAt(0));
		}

		return s.codePoints().noneMatch(Character::isISOControl);
	}
}