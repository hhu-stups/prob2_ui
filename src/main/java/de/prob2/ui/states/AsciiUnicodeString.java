package de.prob2.ui.states;

import java.util.Objects;

import de.prob.unicode.UnicodeTranslator;

public final class AsciiUnicodeString {
	private final String ascii;
	private final String unicode;
	
	private AsciiUnicodeString(final String src, final boolean isUnicode) {
		Objects.requireNonNull(src);
		if (isUnicode) {
			this.ascii = UnicodeTranslator.toAscii(src);
			this.unicode = src;
		} else {
			this.ascii = src;
			this.unicode = UnicodeTranslator.toUnicode(src);
		}
	}
	
	public static AsciiUnicodeString fromAscii(final String s) {
		return new AsciiUnicodeString(s, false);
	}
	
	public static AsciiUnicodeString fromUnicode(final String s) {
		return new AsciiUnicodeString(s, true);
	}
	
	public String toAscii() {
		return this.ascii;
	}
	
	public String toUnicode() {
		return this.unicode;
	}
	
	@Override
	public String toString() {
		return String.format("%s[ascii=%s, unicode=%s]", this.getClass().getName(), this.toAscii(), this.toUnicode());
	}
}
