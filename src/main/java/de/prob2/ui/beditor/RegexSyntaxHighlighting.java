package de.prob2.ui.beditor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.be4.classicalb.core.parser.util.Utils;
import de.prob.scripting.*;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import static java.util.Map.entry;

public final class RegexSyntaxHighlighting {

	private static final Map<Class<? extends ModelFactory<?>>, Map<String, Pattern>> SYNTAX_CLASSES_OTHER_LANGUAGES;

	static {
		//Event-B Regex
		Map<String, Pattern> syntaxClassesForEventB = Map.ofEntries(
			entry("editor_keyword", Pattern.compile("(CONTEXT|EXTENDS|SETS|CONSTANTS|CONCRETE_CONSTANTS|AXIOMS|THEOREMS|MACHINE|REFINES|SEES|VARIABLES|ABSTRACT_VARIABLES|INVARIANT|VARIANT|EVENTS|EVENT|BEGIN|ANY|WHERE|WHEN|WITH|THEN|INITIALISATION|END)", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_ctrlkeyword", Pattern.compile("((POW|POW1|card|union|inter|min|max|finite|partition|dom|ran)|[ |\t|\n|\r]*(UNION|INTER|id|prj1|prj2|skip)[ |\t|\n|\r])", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_logical", Pattern.compile("([ |\t|\n|\r]*(false|true|or|not)[ |\t|\n|\r]|⊤|⊥|&|∧|∨|=>|⇒|<=>|⇔|!|#|¬|∃|∀)", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_assignments", Pattern.compile("(:\\||::|:∈|:=)", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_arithmetic", Pattern.compile("(\\/\\\\|\\\\\\/|∩|∪|\\{\\}|∅|\\\\|\\+->>|⤀|\\+->|⇸|\\+|-->>|↠|-->|→|-|>=|<=|≤|≥|/<<:|<<:|/<:|<:|⊄|⊂|⊈|⊆|/:|:|∉|∈|>\\+>|⤔|>->>|⤖|>->|↣|><|⊗|>|<\\+|⇷|<<->>|<<->|<->>|<->|↔|<<\\||⩤|<\\||◁|<|;|[ |\t|\n|\r]*circ[ |\t\n\r]|◦|\\|>>|⩥|\\|>|▷|\\|\\||%|≠|/=|=)", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_comment", Pattern.compile("(//[^\n\r]*)|(/\\*([^*]|\\*+[^*/])*\\*+/)", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_identifier", Pattern.compile("[_a-zA-Z][_a-zA-Z0-9]*", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ)),
			entry("editor_ignored", Pattern.compile("( |\t|\r|\n)+", Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ))
		);

		//XTL Regex
		Map<String, Pattern> syntaxClassesForXTL = Map.ofEntries(
			entry("editor_keyword", Pattern.compile("(start|trans|prop|heuristic_function_result|heuristic_function_active|prob_pragma_string|animation_(function_result|image|image_right_click_transition|image_click_transition))")),
			entry("editor_types", Pattern.compile("(true|fail|atomic|compound|nonvar|var|functor|arg|op|is|ground|number|copy_term dif|member|memberchk|append|length|nonmember|keysort|term_variables|reverse|last|delete|select|selectchk|maplist|nth|nth1|nth0|perm|perm2|permutation|same_length|add_error|print|write|sort)")),
			entry("editor_string", Pattern.compile("((\"(.*)*\")|('(.*)*'))")),
			entry("editor_xtl_variable", Pattern.compile("[A-Z][_a-zA-Z0-9]*")),
			entry("editor_xtl_functor", Pattern.compile("[_a-z][_a-zA-Z0-9]*")),
			entry("editor_assignments", Pattern.compile(":-|!|-->|;|\\.")),
			entry("editor_comment", Pattern.compile("(%(.)*|/\\*([^*]|\\*+[^*/])*\\*+/)")),
			entry("editor_ignored", Pattern.compile("( |\t|\r|\n)+"))
		);

		//TLA Regex
		Map<String, Pattern> syntaxClassesForTLA = Map.ofEntries(
			entry("editor_keyword", Pattern.compile("(MODULE|CONSTANTS|CONSTANT|ASSUME|ASSUMPTION|VARIABLES|VARIABLE|AXIOM|THEOREM|EXTENDS|INSTANCE|LOCAL)")),
			entry("editor_ctrlkeyword", Pattern.compile("(IF|THEN|ELSE|UNION|CHOOSE|LET|IN|UNCHANGED|SUBSET|CASE|DOMAIN|EXCEPT|ENABLED|SF_|WF_|WITH|OTHER|BOOLEAN|STRING)")),
			entry("editor_types", Pattern.compile("(Next|Init|Spec|Inv)")),
			entry("editor_comment", Pattern.compile("(\\\\\\*[^\n\r]*)|(\\(\\*([^*]|\\*+[^*)])*\\*+\\))")),
			entry("editor_arithmetic", Pattern.compile("\\+|=|-|\\*|\\^|/|\\.\\.|\\\\o|\\\\circ|\\\\div|\\\\leq|\\\\geq|%|<|>|/|Int|Nat")),
			entry("editor_logical", Pattern.compile("<=>|=>|<<|>>|!|#|/=|~|<>|->|->|~\\\\|\"|\\[\\]|TRUE|FALSE|SubSeq|Append|Len|Seq|Head|Tail|Cardinality|IsFiniteSet|/\\\\|\\\\/|\\\\land|\\\\lor|\\\\lnot|\\\\neg|\\\\equiv|\\\\E|\\\\A|\\\\in|\\\\notin|\\\\cap|\\\\intersect|\\\\cup|\\\\subseteq|\\\\subset|\\\\times|\\\\union|\\.|\\\\")),
			entry("editor_identifier", Pattern.compile("[_a-zA-Z][_a-zA-Z0-9]*")),
			entry("editor_ignored", Pattern.compile("( |\t|\r|\n)+"))
		);

		//CSP Regex
		Map<String, Pattern> syntaxClassesForCSP = Map.ofEntries(
			entry("editor_keyword", Pattern.compile("if|then|else|@@|let|within|\\{|\\}|<->|<-|\\[\\||\\|\\]|\\[|\\]|\\\\")),
			entry("editor_types", Pattern.compile("!|\\?|->|\\[\\]|\\|~\\||\\|\\|\\||;|STOP|SKIP|CHAOS|/\\|\\[>|@")),
			entry("editor_arithmetic", Pattern.compile("agent|MAIN|channel|datatype|subtype|nametype|machine|Events")),
			entry("editor_assignments", Pattern.compile("assert|transparent|diamond|print|include")),
			entry("editor_logical", Pattern.compile("true|false|length|null|head|tail|concat|set|Set|Seq|elem|empty|card|member|union|diff|inter|Union|Inter|not|and|or|mod|\\*|\\+|/|==|\\!=|>|<|<=|>=|=<|&&|\\|\\||Int|Bool")),
			entry("editor_unsupported", Pattern.compile("external|extensions|productions|Proc")),
			entry("editor_identifier", Pattern.compile("[_a-zA-Z][_a-zA-Z0-9]*")),
			entry("editor_comment", Pattern.compile("(\\{-([^-]|-+[^\\-\\}])*-+\\})|(--(.*))")),
			entry("editor_ignored", Pattern.compile("( |\t|\r|\n)+"))
		);

		//Alloy Regex
		Map<String, Pattern> syntaxClassesForAlloy = Map.ofEntries(
			entry("editor_keyword", Pattern.compile("module|sig|fact|extends|run|abstract|open|fun|pred|check|assert|plus|minus|mul|div|rem|sum")),
			entry("editor_types", Pattern.compile("not|one|lone|set|no|all|some|disjoint|let|in|for|and|or|implies|iff|else|none|univ|iden|Int|int|=>|&&|<=>|\\|\\||!|\\.|\\^|\\*|<:|:>|\\+\\+|\\~|->|&|\\+|-|=|\\#")),
			entry("editor_identifier", Pattern.compile("[_a-zA-Z][_a-zA-Z0-9]*")),
			entry("editor_comment", Pattern.compile("(//[^\n\r]*|/\\*([^*]|\\*+[^*/])*\\*+/)"))
		);

		//Z Regex
		Map<String, Pattern> syntaxClassesForZ = Map.ofEntries(
			entry("editor_arithmetic", Pattern.compile("(head|tail|last|front|squash|rev|min|max|first|second|succ|count|items|\\\\(\\{|\\}|notin|in|inbag|(big)?cup|(big)?cap|subset|subseteq|subbageq|disjoint|partition|plus|oplus|uplus|uminus|otimes|setminus|emptyset|leq|geq|neq|div|mod|dom|(n)?(d|r)res|langle|rangle|lbag|rbag|ran|id|inv|mapsto|succ|cat|dcat|prefix|suffix|inseq|filter|extract|bcount|\\#))")),
			entry("editor_types", Pattern.compile("\\\\(power(_1)?|nat(_1)?|num|bag|cross|upto|rel|(p)?fun|(p)?inj|bij|seq(_1)?|iseq(_1)?|(b)?tree)")),
			entry("editor_logical", Pattern.compile("\\\\(land|lor|implies|iff|lnot|forall|exists(_1)?|mu|lambda|true|false)")),
			entry("editor_keyword", Pattern.compile("(\\\\(where|also|Delta))|(\\\\(begin|end)\\{(schema|zed|axdef)\\})")),
			entry("editor_assignments", Pattern.compile("::=|=|\\\\(IF|THEN|ELSE|LET|defs)")),
			entry("editor_identifier", Pattern.compile("(\\\\|[_a-zA-Z])[_a-zA-Z0-9]*")),
			entry("editor_comment", Pattern.compile("%(.)*|/\\*([^*]|\\*+[^*/])*\\*+/|\\\\(noindent|documentclass|(begin|end)\\{(document)\\}|(sub)?section|(usepackage)\\{(fuzz|z-eves)\\}|\\\\)")),
			entry("editor_unsupported", Pattern.compile("\\\\(infix|arithmos)"))
		);

		SYNTAX_CLASSES_OTHER_LANGUAGES = Map.ofEntries(
			entry(EventBFactory.class, syntaxClassesForEventB),
			entry(EventBPackageFactory.class, syntaxClassesForEventB),
			entry(XTLFactory.class, syntaxClassesForXTL),
			entry(TLAFactory.class, syntaxClassesForTLA),
			entry(CSPFactory.class, syntaxClassesForCSP),
			entry(AlloyFactory.class, syntaxClassesForAlloy),
			entry(ZFactory.class, syntaxClassesForZ)
		);
	}

	private RegexSyntaxHighlighting() {
		throw new AssertionError("Utility class");
	}

	private record Range(String syntaxClass, int start, int end) {
	}

	static boolean canHighlight(final Class<? extends ModelFactory<?>> modelFactoryClass) {
		return SYNTAX_CLASSES_OTHER_LANGUAGES.containsKey(modelFactoryClass);
	}

	private static List<Range> extractRanges(Map<String, Pattern> syntaxClasses, String text) {
		List<Range> ranges = new ArrayList<>();
		syntaxClasses.forEach((syntaxClass, pattern) -> {
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				String adjustedSyntaxClass;
				if ("editor_identifier".equals(syntaxClass) && Utils.isProBSpecialDefinitionName(matcher.group())) {
					// Recognize and highlight special identifiers (e. g. ANIMATION_FUNCTION, VISB_JSON_FILE)
					adjustedSyntaxClass = "editor_special_identifier";
				} else {
					adjustedSyntaxClass = syntaxClass;
				}
				ranges.add(new Range(adjustedSyntaxClass, matcher.start(), matcher.end()));
			}
		});
		ranges.sort(
			Comparator.comparingInt(Range::start)
				.thenComparing(Comparator.comparingInt(Range::end).reversed())
				.thenComparing(Range::syntaxClass)
		);
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
				if (longest.start != next.start) {
					break;
				}

				if (next.end > longest.end) {
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
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		int pos = 0;
		for (Range range : ranges) {
			if (pos < range.start()) {
				int remainingLength = range.start - pos;
				spansBuilder.add(Collections.singleton("ignored"), remainingLength);
				pos = range.start;
			}

			if (pos == range.start()) {
				int length = range.end - range.start;
				spansBuilder.add(Collections.singleton(range.syntaxClass()), length);
				pos = range.end;
			}
		}

		try {
			return spansBuilder.create();
		} catch (IllegalStateException ignored) {
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	static StyleSpans<Collection<String>> computeHighlighting(final Class<? extends ModelFactory<?>> modelFactoryClass, final String text) {
		Map<String, Pattern> syntaxClasses = SYNTAX_CLASSES_OTHER_LANGUAGES.get(modelFactoryClass);
		List<Range> ranges = extractRanges(syntaxClasses, text);
		List<Range> rangesWithLongestMatch = filterLongestMatch(ranges);
		return createSpansBuilder(rangesWithLongestMatch, text);
	}
}
