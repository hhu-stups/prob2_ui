package de.prob2.ui.beditor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.prob.model.brules.RulesModelFactory;

import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegexSyntaxHighlightingTest {

	@Test
	void regexTestPrefixedWrong() {
		var pattern = Pattern.compile("a|ab");
		var input = "ab";
		var matcher = pattern.matcher(input);
		assertThat(matcher.find()).isTrue();
		assertThat(matcher.group()).isEqualTo("a");
	}

	@Test
	void regexTestPrefixedCorrect() {
		var pattern = Pattern.compile("ab|a");
		var input = "ab";
		var matcher = pattern.matcher(input);
		assertThat(matcher.find()).isTrue();
		assertThat(matcher.group()).isEqualTo("ab");
	}

	@Test
	void ruleTest1() {
		var expected = new StyleSpansBuilder<Collection<String>>()
				.add(Set.of("editor_ctrlkeyword"), 6)
			.create();
		var actual = RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, "RULEID");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void ruleTest2() {
		var expected = new StyleSpansBuilder<Collection<String>>()
				.add(Set.of("editor_ignored"), 6)
				.create();
		var actual = RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, "STRING");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void ruleTest3() {
		var expected = new StyleSpansBuilder<Collection<String>>()
				.add(Set.of("editor_ignored"), 2)
				.create();
		var actual = RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, "IN");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void highlightingTest1() {
		var expected = new StyleSpansBuilder<Collection<String>>()
				.add(Set.of("editor_ignored", "editor_comment"), 3)
				.add(Set.of("editor_ignored", "editor_types"), 6)
				.create();
		var text = "//\nSTRING"; // comment prefix to get into the normal parsing state
		var base = BLexerSyntaxHighlighting.computeBHighlighting(text);
		var overlay = RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, text);
		var actual = base.overlay(overlay, (a, b) -> {
			var c = new HashSet<>(a);
			c.addAll(b);
			return c;
		});
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void highlightingTest2() {
		var expected = new StyleSpansBuilder<Collection<String>>()
				.add(Set.of("editor_ignored", "editor_comment"), 3)
				.add(Set.of("editor_ignored", "editor_ctrlkeyword"), 2)
				.create();
		var text = "//\nIN"; // comment prefix to get into the normal parsing state
		var base = BLexerSyntaxHighlighting.computeBHighlighting(text);
		var overlay = RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, text);
		var actual = base.overlay(overlay, (a, b) -> {
			var c = new HashSet<>(a);
			c.addAll(b);
			return c;
		});
		assertThat(actual).isEqualTo(expected);
	}
}
