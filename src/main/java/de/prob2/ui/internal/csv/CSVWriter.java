package de.prob2.ui.internal.csv;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CSVWriter implements Closeable {

	private static final int COLUMNS_UNKNOWN = -1;

	private final Writer writer;
	private final CSVSettings settings;
	private final boolean flushAfterRecord;

	private int columns = COLUMNS_UNKNOWN;
	private String[] header = null;

	public CSVWriter(Writer writer) {
		this(writer, CSVSettings.RFC_4180);
	}

	public CSVWriter(Writer writer, CSVSettings settings) {
		this(writer, settings, true);
	}

	public CSVWriter(Writer writer, CSVSettings settings, boolean flushAfterRecord) {
		this.writer = Objects.requireNonNull(writer, "writer");
		this.settings = Objects.requireNonNull(settings, "settings");
		this.flushAfterRecord = flushAfterRecord;
	}

	public CSVWriter header(Object... names) throws IOException {
		return header(Arrays.asList(names));
	}

	public CSVWriter header(List<?> names) throws IOException {
		if (names == null || names.isEmpty()) {
			throw new IllegalArgumentException("empty header");
		}

		if (names.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("null header");
		}

		if (header != null) {
			throw new IllegalStateException("header already present");
		}

		header = names.stream().map(Object::toString).toArray(String[]::new);
		return record(names);
	}

	public CSVWriter record(Object... fields) throws IOException {
		return record(Arrays.asList(fields));
	}

	public CSVWriter record(List<?> fields) throws IOException {
		if (fields == null || fields.isEmpty()) {
			throw new IllegalArgumentException("empty record");
		}

		if (fields.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("null field");
		}

		if (header == null && settings.headerRequired()) {
			throw new IllegalStateException("missing header");
		}

		if (columns == COLUMNS_UNKNOWN) {
			columns = fields.size();
		}

		if (columns != fields.size() && settings.sameFieldNumber()) {
			throw new IllegalStateException("different amount of fields in record");
		}

		String record = fields.stream()
			.map(Object::toString)
			.map(settings::quoteAndEscapeIfNecessary)
			.collect(Collectors.joining(String.valueOf(settings.delimiter()), "", settings.lineSeparator()));
		writer.write(record);
		if (flushAfterRecord) {
			writer.flush();
		}

		return this;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
