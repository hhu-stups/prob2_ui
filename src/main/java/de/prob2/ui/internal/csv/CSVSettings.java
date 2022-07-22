package de.prob2.ui.internal.csv;

import java.util.Objects;

public final class CSVSettings {

	public static final CSVSettings RFC_4180 = builder().build();

	private final char delimiter;
	private final char quote;
	private final String lineSeparator;
	private final boolean alwaysQuote;
	private final boolean allowNewLine;
	private final boolean headerRequired;
	private final boolean sameFieldNumber;

	private CSVSettings(char delimiter, char quote, String lineSeparator, boolean alwaysQuote, boolean allowNewLine, boolean headerRequired, boolean sameFieldNumber) {
		this.delimiter = delimiter;
		this.lineSeparator = lineSeparator;
		this.quote = quote;
		this.alwaysQuote = alwaysQuote;
		this.allowNewLine = allowNewLine;
		this.headerRequired = headerRequired;
		this.sameFieldNumber = sameFieldNumber;
	}

	public static Builder builder() {
		return new Builder();
	}

	public char delimiter() {
		return delimiter;
	}

	public char quote() {
		return quote;
	}

	public String lineSeparator() {
		return lineSeparator;
	}

	public boolean alwaysQuote() {
		return alwaysQuote;
	}

	public boolean allowNewLine() {
		return allowNewLine;
	}

	public boolean headerRequired() {
		return headerRequired;
	}

	public boolean sameFieldNumber() {
		return sameFieldNumber;
	}

	public String quoteAndEscapeIfNecessary(String field) {
		if (!allowNewLine && (field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0)) {
			throw new IllegalArgumentException("illegal new line in field");
		}

		boolean hasQuote = field.indexOf(quote) >= 0;
		boolean needsQuote = hasQuote || field.indexOf(delimiter) >= 0 || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
		if (!hasQuote && !needsQuote) {
			return field;
		} else if (!hasQuote) {
			return quote + field + quote;
		} else {
			return quote + field.replace(String.valueOf(quote), String.valueOf(quote) + quote) + quote;
		}
	}

	public static final class Builder {

		private char delimiter = ',';
		private char quote = '"';
		private String lineSeparator = "\r\n";
		private boolean alwaysQuote = false;
		private boolean allowNewLine = true;
		private boolean headerRequired = false;
		private boolean sameFieldNumber = true;

		public Builder delimiter(char delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		public Builder quote(char quote) {
			this.quote = quote;
			return this;
		}

		public Builder lineSeparator(String lineSeparator) {
			this.lineSeparator = Objects.requireNonNull(lineSeparator);
			return this;
		}

		public Builder alwaysQuote(boolean alwaysQuote) {
			this.alwaysQuote = alwaysQuote;
			return this;
		}

		public Builder allowNewLine(boolean allowNewLine) {
			this.allowNewLine = allowNewLine;
			return this;
		}

		public Builder headerRequired(boolean headerRequired) {
			this.headerRequired = headerRequired;
			return this;
		}

		public Builder sameFieldNumber(boolean sameFieldNumber) {
			this.sameFieldNumber = sameFieldNumber;
			return this;
		}

		public CSVSettings build() {
			return new CSVSettings(delimiter, quote, lineSeparator, alwaysQuote, allowNewLine, headerRequired, sameFieldNumber);
		}
	}
}
