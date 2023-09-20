package de.prob2.ui.railml;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static de.prob2.ui.railml.RailMLSvgConverter.convertSvgForVisB;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

public class RailMLSvgConverterTest {

	private static final Path svgPath = Paths.get("src/test/resources/svgs");

	@Test
	void testSimpleExample() throws IOException {

		final Path pathInput = svgPath.resolve("simple_example_dot.svg");
		final Path pathCorrect = svgPath.resolve("simple_example_correct_converted.svg");

		Path pathConverted = Files.createTempFile("railml-",".svg");
		pathConverted.toFile().deleteOnExit();

		Files.copy(pathInput, pathConverted, StandardCopyOption.REPLACE_EXISTING);
		convertSvgForVisB(pathConverted.toString(), "DOT");

		assertThat(Files.readString(pathCorrect))
			.and(Files.readString(pathConverted))
			.ignoreComments()
			.ignoreWhitespace()
			.areSimilar();
	}
}
