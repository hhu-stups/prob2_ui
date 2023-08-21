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
	
	public Checked or(Checked other) {
		if (this == PARSE_ERROR || other == PARSE_ERROR) {
			// Special case: always propagate parse errors,
			// even if the other operand is not an error,
			// because parse errors indicate a configuration problem
			// and should never appear in a correctly configured task.
			return PARSE_ERROR;
		} else if (this == SUCCESS) {
			return this;
		} else if (this == FAIL) {
			return other;
		} else if (other == FAIL) {
			return this;
		} else if (this == TIMEOUT || this == INTERRUPTED || this == LIMIT_REACHED) {
			return other;
		} else if (other == TIMEOUT || other == INTERRUPTED || other == LIMIT_REACHED) {
			return this;
		} else {
			return other;
		}
	}
}
