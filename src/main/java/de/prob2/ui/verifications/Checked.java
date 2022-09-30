package de.prob2.ui.verifications;

public enum Checked {
	NOT_CHECKED, SUCCESS, FAIL, TIMEOUT, INTERRUPTED, PARSE_ERROR, LIMIT_REACHED, UNKNOWN;
	
	public Checked and(final Checked other) {
		if (this == PARSE_ERROR || this == FAIL) {
			return this;
		} else if (other == PARSE_ERROR || other == FAIL) {
			return other;
		} else if (this == TIMEOUT || this == INTERRUPTED || this == LIMIT_REACHED) {
			return this;
		} else if (this == SUCCESS) {
			return other;
		} else {
			return this;
		}
	}
}
