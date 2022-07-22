package de.prob2.ui.internal.csv;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CSVWriterTest {

	@Test
	void testSimple() throws IOException {
		StringWriter w = new StringWriter();
		try (CSVWriter csvWriter = new CSVWriter(w)) {
			csvWriter
					.header("Foo", "Bar")
					.record("A", "B")
					.record("C", "D");
		}

		assertThat(w.toString()).isEqualTo(
				"Foo,Bar\r\n" +
						"A,B\r\n" +
						"C,D\r\n"
		);
	}

	@Test
	void testQuote() throws IOException {
		StringWriter w = new StringWriter();
		try (CSVWriter csvWriter = new CSVWriter(w)) {
			csvWriter
					.record("A,B", "C");
		}

		assertThat(w.toString()).isEqualTo(
				"\"A,B\",C\r\n"
		);
	}

	@Test
	void testQuoteEscape() throws IOException {
		StringWriter w = new StringWriter();
		try (CSVWriter csvWriter = new CSVWriter(w)) {
			csvWriter
					.record("\"A\"", "B");
		}

		assertThat(w.toString()).isEqualTo(
				"\"\"\"A\"\"\",B\r\n"
		);
	}
}
