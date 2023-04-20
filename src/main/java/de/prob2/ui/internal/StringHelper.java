package de.prob2.ui.internal;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.KeyEvent;

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

	public static String keyEventToString(KeyEvent e) {
		List<String> components = new ArrayList<>();
		if (e.isConsumed()) {
			components.add("consumed");
		}

		if (e.getCharacter() != null && !e.getCharacter().isEmpty()) {
			components.add("character=" + StringHelper.escapeNonAscii(e.getCharacter()));
		}

		if (e.getText() != null && !e.getText().isEmpty()) {
			components.add("text=" + StringHelper.escapeNonAscii(e.getText()));
		}

		if (e.getCode() != null) {
			components.add("code=" + e.getCode());
		}

		if (e.isShiftDown()) {
			components.add("shift");
		}

		if (e.isControlDown()) {
			components.add("ctrl");
		}

		if (e.isAltDown()) {
			components.add("alt");
		}

		if (e.isMetaDown()) {
			components.add("meta");
		}

		if (e.isShortcutDown()) {
			components.add("shortcut");
		}

		return e.getClass().getSimpleName() + '[' + e.getEventType() + ']' + '{' + String.join(",", components) + '}';
	}
}
