package de.prob2.ui.beditor;

import java.util.Collection;
import java.util.Collections;
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
	void ruleTest() {
		var expected = new StyleSpansBuilder<Collection<String>>()
			.add(Collections.singleton("editor_ctrlkeyword"), 6)
			.create();
		var actual = RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, "RULEID");
		assertThat(actual).isEqualTo(expected);
	}
}
