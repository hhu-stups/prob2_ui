package de.prob2.ui.consoles;

import java.util.Collection;
import java.util.Objects;

final class CachedMessage {

	private final String text;
	private final Collection<String> style;

	CachedMessage(String text, Collection<String> style) {
		this.text = Objects.requireNonNull(text, "text");
		this.style = Objects.requireNonNull(style, "style");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof CachedMessage)) {
			return false;
		}

		CachedMessage that = (CachedMessage) o;
		return Objects.equals(text, that.text) && Objects.equals(style, that.style);
	}

	@Override
	public int hashCode() {
		return Objects.hash(text, style);
	}
}
