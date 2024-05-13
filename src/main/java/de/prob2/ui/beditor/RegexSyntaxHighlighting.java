package de.prob2.ui.beditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.be4.classicalb.core.parser.util.Utils;
import de.prob.model.brules.RulesModelFactory;
import de.prob.scripting.AlloyFactory;
import de.prob.scripting.CSPFactory;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.scripting.ModelFactory;
import de.prob.scripting.TLAFactory;
import de.prob.scripting.XTLFactory;
import de.prob.scripting.ZFactory;
import de.prob.scripting.ZFuzzFactory;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;

public final class RegexSyntaxHighlighting {

	private static final Map<Class<? extends ModelFactory<?>>, List<TokenClass>> TOKEN_CLASSES;
	private static final Collection<String> IGNORED_STYLE = Collections.singleton("editor_ignored");

	static {
		// unicode flags
		int U = Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ;

		// IMPORTANT: put longer words first in RegEx alternatives!

		//B-Rules DSL keywords Regex
		var tokenClassesForRulesDSL = List.of(
				new TokenClass("editor_keyword", compile("RULES_MACHINE|REFERENCES", U)),
				new TokenClass("editor_ctrlkeyword", compile("RULE_FORALL|RULE_FAIL|RULEID|RULE|DEPENDS_ON_RULE|DEPENDS_ON_COMPUTATION|ACTIVATION|REPLACES|ERROR_TYPES|CLASSIFICATION|TAGS|BODY|EXPECT|ERROR_TYPE|COUNTEREXAMPLE|COMPUTATION|DEFINE|TYPE|DUMMY_VALUE|VALUE|FUNCTION|PRECONDITION|POSTCONDITION|FOR|IN|DO", U)),
				new TokenClass("editor_special_identifier", compile("SUCCEEDED_RULE(?:_ERROR_TYPE)?|GET_RULE_COUNTEREXAMPLES|FAILED_RULE(?:_ERROR_TYPE|_ALL_ERROR_TYPES)?|NOT_CHECKED_RULE|DISABLED_RULE|STRING_FORMAT"))
		);

		//Event-B Regex
		var tokenClassesForEventB = List.of(
				new TokenClass("editor_keyword", compile("CONTEXT|EXTENDS|SETS|CONSTANTS|CONCRETE_CONSTANTS|AXIOMS|THEOREMS|MACHINE|REFINES|SEES|VARIABLES|ABSTRACT_VARIABLES|INVARIANT|VARIANT|EVENTS|EVENT|BEGIN|ANY|WHERE|WHEN|WITH|THEN|INITIALISATION|END", U)),
				new TokenClass("editor_ctrlkeyword", compile("POW1|POW|card|union|inter|min|max|finite|partition|dom|ran|UNION|INTER|id|prj1|prj2|skip", U)),
				new TokenClass("editor_logical", compile("false|true|or|not|⊤|⊥|&|∧|∨|=>|⇒|<=>|⇔|!|#|¬|∃|∀", U)),
				new TokenClass("editor_assignments", compile(":[|:∈=]", U)),
				new TokenClass("editor_arithmetic", compile("/\\\\|\\\\/|∩|∪|\\{}|∅|\\\\|\\+->>|⤀|\\+->|⇸|\\+|-->>|↠|-->|→|-|>=|<=|≤|≥|/<<:|<<:|/<:|<:|⊄|⊂|⊈|⊆|/:|:|∉|∈|>\\+>|⤔|>->>|⤖|>->|↣|><|⊗|>|<\\+|⇷|<<->>|<<->|<->>|<->|↔|<<\\||⩤|<\\||◁|<|;|circ|◦|\\|>>|⩥|\\|>|▷|\\|\\||%|≠|/=|=", U)),
				new TokenClass("editor_comment", compile("//[^\n\r]*|/\\*.*?\\*/", U | Pattern.DOTALL)),
				new TokenClass("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*", U))
		);

		//XTL Regex
		var tokenClassesForXTL = List.of(
				new TokenClass("editor_keyword", compile("start|trans|prop|heuristic_function_result|heuristic_function_active|prob_pragma_string|animation_(?:function_result|image|image_right_click_transition|image_click_transition)")),
				new TokenClass("editor_types", compile("true|fail|atomic|compound|nonvar|var|functor|arg|op|is|ground|number|copy_term|dif|member|memberchk|append|length|nonmember|keysort|term_variables|reverse|last|delete|select|selectchk|maplist|nth1|nth0|nth|perm2|perm|permutation|same_length|add_error|print|write|sort")),
				new TokenClass("editor_string", compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'")),
				new TokenClass("editor_xtl_variable", compile("[_A-Z][_a-zA-Z0-9]*")),
				new TokenClass("editor_xtl_functor", compile("[a-z][_a-zA-Z0-9]*")),
				new TokenClass("editor_assignments", compile(":-|!|-->|;|\\.")),
				new TokenClass("editor_comment", compile("%[^\n\r]*|/\\*.*?\\*/", Pattern.DOTALL))
		);

		//TLA Regex
		var tokenClassesForTLA = List.of(
				new TokenClass("editor_keyword", compile("MODULE|CONSTANTS|CONSTANT|ASSUME|ASSUMPTION|VARIABLES|VARIABLE|AXIOM|THEOREM|EXTENDS|INSTANCE|LOCAL")),
				new TokenClass("editor_ctrlkeyword", compile("IF|THEN|ELSE|UNION|CHOOSE|LET|IN|UNCHANGED|SUBSET|CASE|DOMAIN|EXCEPT|ENABLED|SF_|WF_|WITH|OTHER|BOOLEAN|STRING")),
				new TokenClass("editor_types", compile("Next|Init|Spec|Inv")),
				new TokenClass("editor_comment", compile("\\\\\\*[^\n\r]*|\\(\\*.*?\\*\\)", Pattern.DOTALL)),
				new TokenClass("editor_arithmetic", compile("\\+|=|-|\\*|\\^|/|\\.\\.|\\\\o|\\\\circ|\\\\div|\\\\leq|\\\\geq|%|<|>|Int|Nat|Real")),
				new TokenClass("editor_logical", compile("<=>|=>|<<|>>|!|#|/=|~|<>|->|~\\\\|\"|\\[]|TRUE|FALSE|SubSeq|Append|Len|Seq|Head|Tail|Cardinality|IsFiniteSet|/\\\\|\\\\/|\\\\land|\\\\lor|\\\\lnot|\\\\neg|\\\\equiv|\\\\E|\\\\A|\\\\in|\\\\notin|\\\\cap|\\\\intersect|\\\\cup|\\\\subseteq|\\\\subset|\\\\times|\\\\union|\\.|\\\\")),
				new TokenClass("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*"))
		);

		//CSP Regex
		var tokenClassesForCSP = List.of(
				new TokenClass("editor_keyword", compile("if|then|else|@@|let|within|\\{|}|<->|<-|\\[\\||\\|]|\\[|]|\\\\")),
				new TokenClass("editor_types", compile("!|\\?|->|\\[]|\\|~\\||\\|\\|\\||;|STOP|SKIP|CHAOS|/\\|\\[>|@")),
				new TokenClass("editor_arithmetic", compile("agent|MAIN|channel|datatype|subtype|nametype|machine|Events")),
				new TokenClass("editor_assignments", compile("assert|transparent|diamond|print|include")),
				new TokenClass("editor_logical", compile("true|false|length|null|head|tail|concat|set|Set|Seq|elem|empty|card|member|union|diff|inter|Union|Inter|not|and|or|mod|\\*|\\+|/|==|!=|<=|>=|=<|>|<|&&|\\|\\||Int|Bool")),
				new TokenClass("editor_unsupported", compile("external|extensions|productions|Proc")),
				new TokenClass("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*")),
				new TokenClass("editor_comment", compile("--[^\n\r]*|\\{-.*?-}", Pattern.DOTALL))
		);

		//Alloy Regex
		var tokenClassesForAlloy = List.of(
				new TokenClass("editor_keyword", compile("module|sig|fact|extends|run|abstract|open|fun|pred|check|assert|plus|minus|mul|div|rem|sum")),
				new TokenClass("editor_types", compile("not|one|lone|set|no|all|some|disjoint|let|in|for|and|or|implies|iff|else|none|univ|iden|Int|int|=>|&&|<=>|\\|\\||!|\\.|\\^|\\*|<:|:>|\\+\\+|~|->|&|\\+|-|=|#")),
				new TokenClass("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*")),
				new TokenClass("editor_comment", compile("//[^\n\r]*|/\\*.*?\\*/", Pattern.DOTALL))
		);

		//Z Regex
		var tokenClassesForZ = List.of(
				new TokenClass("editor_arithmetic", compile("head|tail|last|front|squash|rev|min|max|first|second|succ|count|items|\\\\(?:\\{|}|notin|in|inbag|(?:big)?(?:cup|cap)|subset|subseteq|subbageq|disjoint|partition|plus|oplus|uplus|uminus|otimes|setminus|emptyset|leq|geq|neq|div|mod|dom|n?[dr]res|langle|rangle|lbag|rbag|ran|id|inv|mapsto|succ|cat|dcat|prefix|suffix|inseq|filter|extract|bcount|#)")),
				new TokenClass("editor_types", compile("\\\\(?:power(?:_1)?|nat(?:_1)?|num|bag|cross|upto|rel|p?fun|p?inj|bij|seq(?:_1)?|iseq(?:_1)?|b?tree)")),
				new TokenClass("editor_logical", compile("\\\\(?:land|lor|implies|iff|lnot|forall|exists(?:_1)?|mu|lambda|true|false)")),
				new TokenClass("editor_keyword", compile("\\\\(?:where|also|Delta)|\\\\(?:begin|end)\\{(?:schema|zed|axdef)}")),
				new TokenClass("editor_assignments", compile("::=|=|\\\\(?:IF|THEN|ELSE|LET|defs)")),
				new TokenClass("editor_identifier", compile("(?:\\\\|[_a-zA-Z])[_a-zA-Z0-9]*")),
				new TokenClass("editor_comment", compile("%[^\n\r]*|/\\*.*?\\*/|\\\\(?:noindent|documentclass|(?:begin|end)\\{document}|(?:sub)?section|usepackage\\{(?:fuzz|z-eves)}|\\\\)", Pattern.DOTALL)),
				new TokenClass("editor_unsupported", compile("\\\\(?:infix|arithmos)"))
		);

		TOKEN_CLASSES = Map.ofEntries(
				entry(ClassicalBFactory.class, tokenClassesForEventB), // fallback
				entry(RulesModelFactory.class, tokenClassesForRulesDSL),
				entry(EventBFactory.class, tokenClassesForEventB),
				entry(EventBPackageFactory.class, tokenClassesForEventB),
				entry(XTLFactory.class, tokenClassesForXTL),
				entry(TLAFactory.class, tokenClassesForTLA),
				entry(CSPFactory.class, tokenClassesForCSP),
				entry(AlloyFactory.class, tokenClassesForAlloy),
				entry(ZFactory.class, tokenClassesForZ),
				entry(ZFuzzFactory.class, tokenClassesForZ)
		);
	}

	private RegexSyntaxHighlighting() {
		throw new AssertionError("Utility class");
	}

	static boolean canHighlight(final Class<? extends ModelFactory<?>> modelFactoryClass) {
		return TOKEN_CLASSES.containsKey(modelFactoryClass);
	}

	private static List<Range> extractRanges(List<TokenClass> tokenClasses, String text) {
		List<Range> ranges = new ArrayList<>();
		for (var tokenClass : tokenClasses) {
			if (Thread.currentThread().isInterrupted()) {
				return Collections.emptyList();
			}

			Matcher matcher = tokenClass.pattern().matcher(text);
			while (matcher.find()) {
				String adjustedSyntaxClass;
				if ("editor_identifier".equals(tokenClass.syntaxClass()) && Utils.isProBSpecialDefinitionName(matcher.group())) {
					// Recognize and highlight special identifiers (e.g. ANIMATION_FUNCTION, VISB_JSON_FILE)
					adjustedSyntaxClass = "editor_special_identifier";
				} else {
					adjustedSyntaxClass = tokenClass.syntaxClass();
				}
				ranges.add(new Range(adjustedSyntaxClass, matcher.start(), matcher.end()));
			}
		}

		// stable sort: we are keeping the order of matches when the range is the same!
		ranges.sort(Comparator.comparingInt(Range::start).thenComparingInt(Range::end));
		return ranges;
	}

	/**
	 * Removes matches that are contained inside other matches.
	 * Assumes the input is sorted by {@link Range#start} and {@link Range#end}.
	 */
	private static List<Range> filterLongestMatch(List<Range> ranges) {
		int size = ranges.size();
		List<Range> rangesWithLongestMatch = new ArrayList<>();
		int i = 0;
		while (i < size) {
			Range longest = ranges.get(i);
			int j = i + 1;
			while (j < size) {
				Range next = ranges.get(j);
				assert next.start() >= longest.start();
				if (next.start() != longest.start() && next.end() > longest.end()) {
					// next has a part that is after our current longest token but also misses the beginning
					// we accept both then
					break;
				}

				if (next.end() > longest.end()) {
					longest = next;
				}
				j++;
			}
			rangesWithLongestMatch.add(longest);
			i = j;
		}
		return rangesWithLongestMatch;
	}

	private static StyleSpans<Collection<String>> createSpansBuilder(List<Range> ranges, String text) {
		var spansBuilder = new StyleSpansBuilder<Collection<String>>();
		int pos = 0;
		for (Range range : ranges) {
			if (pos < range.start()) {
				int remainingLength = range.start() - pos;
				spansBuilder.add(IGNORED_STYLE, remainingLength);
				pos = range.start();
			}

			if (pos == range.start()) {
				int length = range.end() - range.start();
				spansBuilder.add(Collections.singleton(range.syntaxClass()), length);
				pos = range.end();
			}
			// else: pos > range.start()
			// => ignore these ranges
		}

		if (pos < text.length()) {
			spansBuilder.add(IGNORED_STYLE, text.length() - pos);
		}

		try {
			return spansBuilder.create();
		} catch (IllegalStateException ignored) {
			// this exception is only thrown when there were no spans
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	static StyleSpans<Collection<String>> computeHighlighting(final Class<? extends ModelFactory<?>> modelFactoryClass, final String text) {
		var tokenClasses = TOKEN_CLASSES.get(modelFactoryClass);
		var ranges = extractRanges(tokenClasses, text);
		var rangesWithLongestMatch = filterLongestMatch(ranges);
		return createSpansBuilder(rangesWithLongestMatch, text);
	}

	private record TokenClass(String syntaxClass, Pattern pattern) {
		TokenClass {
			Objects.requireNonNull(syntaxClass, "syntaxClass");
			Objects.requireNonNull(pattern, "pattern");
		}
	}

	private record Range(String syntaxClass, int start, int end) {
		Range {
			Objects.requireNonNull(syntaxClass, "syntaxClass");
			if (start < 0 || start >= end) {
				throw new IllegalArgumentException("0 <= start < end");
			}
		}
	}
}
