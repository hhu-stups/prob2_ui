package de.prob2.ui.internal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.prob2.ui.internal.StringUtil.snakeCaseToCamelCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

class StringUtilTest {

	@Nested
	class SnakeCaseToCamelCaseTest {

		@Test
		void testNull() {
			assertThatRuntimeException().isThrownBy(() -> snakeCaseToCamelCase(null));
		}

		@Test
		void testEmpty() {
			assertThat(snakeCaseToCamelCase("")).isEqualTo("");
		}

		@Test
		void testLower() {
			assertThat(snakeCaseToCamelCase("foo")).isEqualTo("foo");
		}

		@Test
		void testUpper() {
			assertThat(snakeCaseToCamelCase("FOO")).isEqualTo("foo");
		}

		@Test
		void testCamelCase() {
			assertThat(snakeCaseToCamelCase("fooBar")).isEqualTo("foobar");
		}

		@Test
		void testPascalCase() {
			assertThat(snakeCaseToCamelCase("FooBar")).isEqualTo("foobar");
		}

		@Test
		void testSnakeCase() {
			assertThat(snakeCaseToCamelCase("foo_bar")).isEqualTo("fooBar");
		}

		@Test
		void testScreamingSnakeCase() {
			assertThat(snakeCaseToCamelCase("FOO_BAR")).isEqualTo("fooBar");
		}

		@Test
		void testKebabCase() {
			assertThat(snakeCaseToCamelCase("foo-bar")).isEqualTo("foo-bar");
		}

		@Test
		void testTrainCase() {
			assertThat(snakeCaseToCamelCase("FOO-BAR")).isEqualTo("foo-bar");
		}

		@Test
		void testMultiUnderscore() {
			assertThat(snakeCaseToCamelCase("_FOO__BAR_")).isEqualTo("fooBar");
		}

		@Test
		void testGermanS() {
			assertThat(snakeCaseToCamelCase("foo_ß")).isEqualTo("fooSS");
		}

		@Test
		void testGermanUmlaut() {
			assertThat(snakeCaseToCamelCase("foo_ä")).isEqualTo("fooÄ");
		}
	}
}
