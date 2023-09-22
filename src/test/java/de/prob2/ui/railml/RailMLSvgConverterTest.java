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

	@Test
	void testCreationOfIDs() throws IOException {
		//***** Input SVG *****/
		String inputSvg = """
			<svg>
				<g>
					<g id="test_id">
						<ellipse test_attr1="dummy1"/>
						<text test_attr2="dummy2"/>
						<title test_attr3="dummy3"/>
					</g>
				</g>
			</svg>""";

		Path pathConverted = Files.createTempFile("railml-",".svg");
		pathConverted.toFile().deleteOnExit();
		Files.writeString(pathConverted, inputSvg);
		//***** *****/

		//***** Expected SVG *****/
		String expectedSvg = """
			<svg>
				<defs/>
				<g>
					<g id="test_id">
						<ellipse id="test_id_ellipse" test_attr1="dummy1"/>
						<text id="test_id_text" test_attr2="dummy2"/>
						<title id="test_id_title" test_attr3="dummy3">test_id</title>
					</g>
				</g>
			</svg>""";
		//***** *****/

		convertSvgForVisB(pathConverted.toString(), "DOT");

		assertThat(Files.readString(pathConverted))
			.and(expectedSvg)
			.ignoreComments()
			.ignoreWhitespace()
			.areSimilar();
	}

	@Test
	void testPathConversionForDotMode() throws IOException {
		//***** Input SVG *****/
		String inputSvg = """
			<svg>
				<g>
					<g id="path_01">
						<path d="M250,10C30,50 50,50 Q 70,50 L 100,50Z" />
					</g>
				</g>
			</svg>""";

		Path pathConverted = Files.createTempFile("railml-",".svg");
		pathConverted.toFile().deleteOnExit();
		Files.writeString(pathConverted, inputSvg);
		//***** *****/

		//***** Expected SVG *****/
		String expectedSvg = """
			<svg>
				<defs>
					<linearGradient id="path_01_lg_occ" gradientUnits="userSpaceOnUse" x1="250" y1="10" x2="100" y2="50">
						<stop id="path_01_lg_occ_1" offset="0%" style="stop-opacity:0" />
						<stop id="path_01_lg_occ_2" offset="0%" style="stop-color:red; stop-opacity:1" />
						<stop id="path_01_lg_occ_3" offset="100%" style="stop-color:red; stop-opacity:1" />
						<stop id="path_01_lg_occ_4" offset="100%" style="stop-opacity:0" />
					</linearGradient>
					<linearGradient id="path_01_lg_ovl" gradientUnits="userSpaceOnUse" x1="250" y1="10" x2="100" y2="50">
						<stop id="path_01_lg_ovl_1" offset="0%" style="stop-opacity:0" />
						<stop id="path_01_lg_ovl_2" offset="0%" style="stop-color:mediumvioletred; stop-opacity:1" />
						<stop id="path_01_lg_ovl_3" offset="100%" style="stop-color:mediumvioletred; stop-opacity:1" />
						<stop id="path_01_lg_ovl_4" offset="100%" style="stop-opacity:0" />
					</linearGradient>
					<linearGradient id="path_01_lg_res" gradientUnits="userSpaceOnUse" x1="250" y1="10" x2="100" y2="50">
						<stop id="path_01_lg_res_1" offset="0%" style="stop-opacity:0" />
						<stop id="path_01_lg_res_2" offset="0%" style="stop-color:darkorange; stop-opacity:1" />
						<stop id="path_01_lg_res_3" offset="100%" style="stop-color:darkorange; stop-opacity:1" />
						<stop id="path_01_lg_res_4" offset="100%" style="stop-opacity:0" />
					</linearGradient>
					<linearGradient id="path_01_lg_tvd" gradientUnits="userSpaceOnUse" x1="250" y1="10" x2="100" y2="50">
						<stop id="path_01_lg_tvd_1" offset="0%" style="stop-opacity:0" />
						<stop id="path_01_lg_tvd_2" offset="0%" style="stop-color:blue; stop-opacity:1" />
						<stop id="path_01_lg_tvd_3" offset="100%" style="stop-color:blue; stop-opacity:1" />
						<stop id="path_01_lg_tvd_4" offset="100%" style="stop-opacity:0" />
					</linearGradient>
					<linearGradient id="path_01_lg_free" gradientUnits="userSpaceOnUse" x1="250" y1="10" x2="100" y2="50">
						<stop id="path_01_lg_free_1" offset="0%" style="stop-color:black; stop-opacity:1" />
						<stop id="path_01_lg_free_2" offset="0%" style="stop-color:yellowgreen; stop-opacity:1" />
						<stop id="path_01_lg_free_3" offset="100%" style="stop-color:yellowgreen; stop-opacity:1" />
						<stop id="path_01_lg_free_4" offset="100%" style="stop-color:black; stop-opacity:1" />
					</linearGradient>
				</defs>
				<g>
					<g class="edge" id="path_01">
						<path d="M 250,10 L 30,50 L 50,50 L  L  L 70,50 L  L  L 100,50" fill="none" id="path_01_free" stroke="url(#path_01_lg_free)" stroke-width="1.33"/>
						<path d="M 250,10 L 30,50 L 50,50 L  L  L 70,50 L  L  L 100,50" fill="none" id="path_01_tvd" stroke="url(#path_01_lg_tvd)" stroke-width="1.5">
							<animate attributeName="opacity" dur="1s" id="path_01_tvd_blink" repeatCount="indefinite" values="1" />
						</path>
						<path d="M 250,10 L 30,50 L 50,50 L  L  L 70,50 L  L  L 100,50" fill="none" id="path_01_res" stroke="url(#path_01_lg_res)" stroke-width="1.67">
							<animate attributeName="opacity" dur="1s" id="path_01_res_blink" repeatCount="indefinite" values="1" />
						</path>
						<path d="M 250,10 L 30,50 L 50,50 L  L  L 70,50 L  L  L 100,50" fill="none" id="path_01_ovl" stroke="url(#path_01_lg_ovl)" stroke-width="1.85">
							<animate attributeName="opacity" dur="1s" id="path_01_ovl_blink" repeatCount="indefinite" values="1" />
						</path>
						<path d="M 250,10 L 30,50 L 50,50 L  L  L 70,50 L  L  L 100,50" fill="none" id="path_01_occ" stroke="url(#path_01_lg_occ)" stroke-width="2.0"/>
			            <title id="path_01_title">path_01</title>
			        </g>
				</g>
			</svg>""";
		//***** *****/

		convertSvgForVisB(pathConverted.toString(), "DOT");

		assertThat(Files.readString(pathConverted))
			.and(expectedSvg)
			.ignoreComments()
			.ignoreWhitespace()
			.areSimilar();
	}
}
