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
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.scripting.ModelFactory;
import de.prob.scripting.TLAFactory;
import de.prob.scripting.XTLFactory;
import de.prob.scripting.ZFactory;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;

public final class RegexSyntaxHighlighting {

	private static final Map<Class<? extends ModelFactory<?>>, List<Token>> SYNTAX_CLASSES_OTHER_LANGUAGES;

	static {
		// unicode flags
		int U = Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ;

		//Event-B Regex
		var syntaxClassesForEventB = List.of(
			new Token("editor_keyword", compile("CONTEXT|EXTENDS|SETS|CONSTANTS|CONCRETE_CONSTANTS|AXIOMS|THEOREMS|MACHINE|REFINES|SEES|VARIABLES|ABSTRACT_VARIABLES|INVARIANT|VARIANT|EVENTS|EVENT|BEGIN|ANY|WHERE|WHEN|WITH|THEN|INITIALISATION|END", U)),
			new Token("editor_ctrlkeyword", compile("(?:POW|POW1|card|union|inter|min|max|finite|partition|dom|ran)|[ \t\n\r]*(UNION|INTER|id|prj1|prj2|skip)[ \t\n\r]", U)),
			new Token("editor_logical", compile("[ \t\n\r]*(false|true|or|not)[ \t\n\r]|⊤|⊥|&|∧|∨|=>|⇒|<=>|⇔|!|#|¬|∃|∀", U)),
			new Token("editor_assignments", compile(":\\||::|:∈|:=", U)),
			new Token("editor_arithmetic", compile("/\\\\|\\\\/|∩|∪|\\{}|∅|\\\\|\\+->>|⤀|\\+->|⇸|\\+|-->>|↠|-->|→|-|>=|<=|≤|≥|/<<:|<<:|/<:|<:|⊄|⊂|⊈|⊆|/:|:|∉|∈|>\\+>|⤔|>->>|⤖|>->|↣|><|⊗|>|<\\+|⇷|<<->>|<<->|<->>|<->|↔|<<\\||⩤|<\\||◁|<|;|[ \t\n\r]*circ[ \t\n\r]|◦|\\|>>|⩥|\\|>|▷|\\|\\||%|≠|/=|=", U)),
			new Token("editor_comment", compile("//[^\n\r]*|/\\*.*?\\*/", U | Pattern.DOTALL)),
			new Token("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*", U)),
			new Token("editor_ignored", compile("[ \t\r\n]+", U))
		);

		//XTL Regex
		var syntaxClassesForXTL = List.of(
			new Token("editor_keyword", compile("start|trans|prop|heuristic_function_result|heuristic_function_active|prob_pragma_string|animation_(?:function_result|image|image_right_click_transition|image_click_transition)")),
			new Token("editor_types", compile("true|fail|atomic|compound|nonvar|var|functor|arg|op|is|ground|number|copy_term dif|member|memberchk|append|length|nonmember|keysort|term_variables|reverse|last|delete|select|selectchk|maplist|nth|nth1|nth0|perm|perm2|permutation|same_length|add_error|print|write|sort")),
			new Token("editor_string", compile("\"[^\"]*\"|'[^']*'")),
			new Token("editor_xtl_variable", compile("[A-Z][_a-zA-Z0-9]*")),
			new Token("editor_xtl_functor", compile("[_a-z][_a-zA-Z0-9]*")),
			new Token("editor_assignments", compile(":-|!|-->|;|\\.")),
			new Token("editor_comment", compile("%[^\n\r]*|/\\*.*?\\*/", Pattern.DOTALL)),
			new Token("editor_ignored", compile("[ \t\r\n]+"))
		);

		//TLA Regex
		var syntaxClassesForTLA = List.of(
			new Token("editor_keyword", compile("MODULE|CONSTANTS|CONSTANT|ASSUME|ASSUMPTION|VARIABLES|VARIABLE|AXIOM|THEOREM|EXTENDS|INSTANCE|LOCAL")),
			new Token("editor_ctrlkeyword", compile("IF|THEN|ELSE|UNION|CHOOSE|LET|IN|UNCHANGED|SUBSET|CASE|DOMAIN|EXCEPT|ENABLED|SF_|WF_|WITH|OTHER|BOOLEAN|STRING")),
			new Token("editor_types", compile("Next|Init|Spec|Inv")),
			new Token("editor_comment", compile("\\\\\\*[^\n\r]*|\\(\\*.*?\\*\\)", Pattern.DOTALL)),
			new Token("editor_arithmetic", compile("\\+|=|-|\\*|\\^|/|\\.\\.|\\\\o|\\\\circ|\\\\div|\\\\leq|\\\\geq|%|<|>|Int|Nat")),
			new Token("editor_logical", compile("<=>|=>|<<|>>|!|#|/=|~|<>|->|~\\\\|\"|\\[]|TRUE|FALSE|SubSeq|Append|Len|Seq|Head|Tail|Cardinality|IsFiniteSet|/\\\\|\\\\/|\\\\land|\\\\lor|\\\\lnot|\\\\neg|\\\\equiv|\\\\E|\\\\A|\\\\in|\\\\notin|\\\\cap|\\\\intersect|\\\\cup|\\\\subseteq|\\\\subset|\\\\times|\\\\union|\\.|\\\\")),
			new Token("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*")),
			new Token("editor_ignored", compile("[ \t\r\n]+"))
		);

		//CSP Regex
		var syntaxClassesForCSP = List.of(
			new Token("editor_keyword", compile("if|then|else|@@|let|within|\\{|\\}|<->|<-|\\[\\||\\|]|\\[|]|\\\\")),
			new Token("editor_types", compile("!|\\?|->|\\[]|\\|~\\||\\|\\|\\||;|STOP|SKIP|CHAOS|/\\|\\[>|@")),
			new Token("editor_arithmetic", compile("agent|MAIN|channel|datatype|subtype|nametype|machine|Events")),
			new Token("editor_assignments", compile("assert|transparent|diamond|print|include")),
			new Token("editor_logical", compile("true|false|length|null|head|tail|concat|set|Set|Seq|elem|empty|card|member|union|diff|inter|Union|Inter|not|and|or|mod|\\*|\\+|/|==|\\!=|>|<|<=|>=|=<|&&|\\|\\||Int|Bool")),
			new Token("editor_unsupported", compile("external|extensions|productions|Proc")),
			new Token("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*")),
			new Token("editor_comment", compile("--[^\n\r]*|\\{-.*?-\\}", Pattern.DOTALL)),
			new Token("editor_ignored", compile("[ \t\r\n]+"))
		);

		//Alloy Regex
		var syntaxClassesForAlloy = List.of(
			new Token("editor_keyword", compile("module|sig|fact|extends|run|abstract|open|fun|pred|check|assert|plus|minus|mul|div|rem|sum")),
			new Token("editor_types", compile("not|one|lone|set|no|all|some|disjoint|let|in|for|and|or|implies|iff|else|none|univ|iden|Int|int|=>|&&|<=>|\\|\\||!|\\.|\\^|\\*|<:|:>|\\+\\+|~|->|&|\\+|-|=|#")),
			new Token("editor_identifier", compile("[_a-zA-Z][_a-zA-Z0-9]*")),
			new Token("editor_comment", compile("//[^\n\r]*|/\\*.*?\\*/", Pattern.DOTALL))
		);

		//Z Regex
		var syntaxClassesForZ = List.of(
			new Token("editor_arithmetic", compile("head|tail|last|front|squash|rev|min|max|first|second|succ|count|items|\\\\(\\{|\\}|notin|in|inbag|(big)?cup|(big)?cap|subset|subseteq|subbageq|disjoint|partition|plus|oplus|uplus|uminus|otimes|setminus|emptyset|leq|geq|neq|div|mod|dom|(n)?(d|r)res|langle|rangle|lbag|rbag|ran|id|inv|mapsto|succ|cat|dcat|prefix|suffix|inseq|filter|extract|bcount|\\#)")),
			new Token("editor_types", compile("\\\\(?:power(?:_1)?|nat(?:_1)?|num|bag|cross|upto|rel|p?fun|p?inj|bij|seq(?:_1)?|iseq(?:_1)?|b?tree)")),
			new Token("editor_logical", compile("\\\\(?:land|lor|implies|iff|lnot|forall|exists(?:_1)?|mu|lambda|true|false)")),
			new Token("editor_keyword", compile("\\\\(?:where|also|Delta)|\\\\(?:begin|end)\\{(?:schema|zed|axdef)\\}")),
			new Token("editor_assignments", compile("::=|=|\\\\(?:IF|THEN|ELSE|LET|defs)")),
			new Token("editor_identifier", compile("(?:\\\\|[_a-zA-Z])[_a-zA-Z0-9]*")),
			new Token("editor_comment", compile("%[^\n\r]*|/\\*.*?\\*/|\\\\(?:noindent|documentclass|(?:begin|end)\\{document\\}|(?:sub)?section|usepackage\\{(?:fuzz|z-eves)\\}|\\\\)", Pattern.DOTALL)),
			new Token("editor_unsupported", compile("\\\\(?:infix|arithmos)"))
		);

		//B-Rules DSL keywords Regex
		var syntaxClassesForRulesDSL = List.of(
			new Token("editor_keyword", compile("\\b(RULES_MACHINE|REFERENCES)\\b", U)),
			new Token("editor_ctrlkeyword", compile("\\b(RULE|DEPENDS_ON_RULE|DEPENDS_ON_COMPUTATION|ACTIVATION|REPLACES|ERROR_TYPES|CLASSIFICATION|RULEID|TAGS|BODY|RULE_FORALL|EXPECT|RULE_FAIL|ERROR_TYPE|COUNTEREXAMPLE|COMPUTATION|DEFINE|TYPE|DUMMY_VALUE|VALUE|FUNCTION|PRECONDITION|POSTCONDITION|FOR|IN|DO)\\b", U)),
			new Token("editor_special_identifier", compile("SUCCEEDED_RULE(?:_ERROR_TYPE)?|GET_RULE_COUNTEREXAMPLES|FAILED_RULE(?:_ERROR_TYPE|_ALL_ERROR_TYPES)?|NOT_CHECKED_RULE|DISABLED_RULE|STRING_FORMAT"))
		);

		SYNTAX_CLASSES_OTHER_LANGUAGES = Map.ofEntries(
			entry(EventBFactory.class, syntaxClassesForEventB),
			entry(EventBPackageFactory.class, syntaxClassesForEventB),
			entry(XTLFactory.class, syntaxClassesForXTL),
			entry(TLAFactory.class, syntaxClassesForTLA),
			entry(CSPFactory.class, syntaxClassesForCSP),
			entry(AlloyFactory.class, syntaxClassesForAlloy),
			entry(ZFactory.class, syntaxClassesForZ),
			entry(RulesModelFactory.class, syntaxClassesForRulesDSL)
		);
	}

	private RegexSyntaxHighlighting() {
		throw new AssertionError("Utility class");
	}

	static boolean canHighlight(final Class<? extends ModelFactory<?>> modelFactoryClass) {
		return SYNTAX_CLASSES_OTHER_LANGUAGES.containsKey(modelFactoryClass);
	}

	private static List<Range> extractRanges(List<Token> syntaxClasses, String text) {
		List<Range> ranges = new ArrayList<>();
		syntaxClasses.forEach(token -> {
			Matcher matcher = token.pattern().matcher(text);
			while (matcher.find()) {
				String adjustedSyntaxClass;
				if ("editor_identifier".equals(token.syntaxClass()) && Utils.isProBSpecialDefinitionName(matcher.group())) {
					// Recognize and highlight special identifiers (e.g. ANIMATION_FUNCTION, VISB_JSON_FILE)
					adjustedSyntaxClass = "editor_special_identifier";
				} else {
					adjustedSyntaxClass = token.syntaxClass();
				}
				ranges.add(new Range(adjustedSyntaxClass, matcher.start(), matcher.end()));
			}
		});

		// stable sort!
		ranges.sort(Comparator.comparingInt(Range::start));
		return ranges;
	}

	/**
	 * Removes matches that start at the same position but are shorter.
	 * Assumes the input is sorted by {@link Range#start}.
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
				if (longest.start() != next.start()) {
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
				spansBuilder.add(Collections.singleton("ignored"), remainingLength);
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

		try {
			return spansBuilder.create();
		} catch (IllegalStateException ignored) {
			// this exception is only thrown when there were no spans
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	static StyleSpans<Collection<String>> computeHighlighting(final Class<? extends ModelFactory<?>> modelFactoryClass, final String text) {
		var syntaxClasses = SYNTAX_CLASSES_OTHER_LANGUAGES.get(modelFactoryClass);
		var ranges = extractRanges(syntaxClasses, text);
		var rangesWithLongestMatch = filterLongestMatch(ranges);
		return createSpansBuilder(rangesWithLongestMatch, text);
	}

	private record Token(String syntaxClass, Pattern pattern) {
		Token {
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
