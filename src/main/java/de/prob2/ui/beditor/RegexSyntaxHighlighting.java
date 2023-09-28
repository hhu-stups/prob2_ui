package de.prob2.ui.beditor;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.be4.classicalb.core.parser.util.Utils;
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

public final class RegexSyntaxHighlighting {
	private static final Map<Class<? extends ModelFactory<?>>, Map<String, String>> syntaxClassesOtherLanguages = new HashMap<>();

	private RegexSyntaxHighlighting() {
		throw new AssertionError("Utility class");
	}

	private static class Range {
		private final String syntaxClass;
		private final int start;
		private final int end;

		public Range(final String syntaxClass, final int start, final int end) {
			this.syntaxClass = syntaxClass;
			this.start = start;
			this.end = end;
		}

		private String getSyntaxClass() {
			return syntaxClass;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
	}

	static {
		//Event-B Regex
		final Map<String, String> syntaxClassesForEventB = new LinkedHashMap<>();
		syntaxClassesForEventB.put("(CONTEXT|EXTENDS|SETS|CONSTANTS|CONCRETE_CONSTANTS|AXIOMS|THEOREMS|MACHINE|REFINES|SEES|VARIABLES|ABSTRACT_VARIABLES|INVARIANT|VARIANT|EVENTS|EVENT|BEGIN|ANY|WHERE|WHEN|WITH|THEN|INITIALISATION|END)", "editor_keyword");
		syntaxClassesForEventB.put("((POW|POW1|card|union|inter|min|max|finite|partition|dom|ran)|[ |\t|\n|\r]*(UNION|INTER|id|prj1|prj2|skip)[ |\t|\n|\r])", "editor_ctrlkeyword");
		syntaxClassesForEventB.put("([ |\t|\n|\r]*(false|true|or|not)[ |\t|\n|\r]|⊤|⊥|&|∧|∨|=>|⇒|<=>|⇔|!|#|¬|∃|∀)", "editor_logical");
		syntaxClassesForEventB.put("(:\\||::|:∈|:=)", "editor_assignments");
		syntaxClassesForEventB.put("(\\/\\\\|\\\\\\/|∩|∪|\\{\\}|∅|\\\\|\\+->>|⤀|\\+->|⇸|\\+|-->>|↠|-->|→|-|>=|<=|≤|≥|/<<:|<<:|/<:|<:|⊄|⊂|⊈|⊆|/:|:|∉|∈|>\\+>|⤔|>->>|⤖|>->|↣|><|⊗|>|<\\+|⇷|<<->>|<<->|<->>|<->|↔|<<\\||⩤|<\\||◁|<|;|[ |\t|\n|\r]*circ[ |\t\n\r]|◦|\\|>>|⩥|\\|>|▷|\\|\\||%|≠|/=|=)", "editor_arithmetic");
		syntaxClassesForEventB.put("(//[^\n\r]*)|(/\\*([^*]|\\*+[^*/])*\\*+/)", "editor_comment");
		syntaxClassesForEventB.put("[_a-zA-Z][_a-zA-Z0-9]*", "editor_identifier");
		syntaxClassesForEventB.put("( |\t|\r|\n)+", "editor_ignored");

		//XTL Regex
		final Map<String, String> syntaxClassesForXTL = new LinkedHashMap<>();
		syntaxClassesForXTL.put("(start|trans|prop|heuristic_function_result|heuristic_function_active|prob_pragma_string|animation_(function_result|image|image_right_click_transition|image_click_transition))", "editor_keyword");
		syntaxClassesForXTL.put("(true|fail|atomic|compound|nonvar|var|functor|arg|op|is|ground|number|copy_term dif|member|memberchk|append|length|nonmember|keysort|term_variables|reverse|last|delete|select|selectchk|maplist|nth|nth1|nth0|perm|perm2|permutation|same_length|add_error|print|write|sort)", "editor_types");
		syntaxClassesForXTL.put("((\"(.*)*\")|('(.*)*'))", "editor_string");
		syntaxClassesForXTL.put("[A-Z][_a-zA-Z0-9]*", "editor_xtl_variable");
		syntaxClassesForXTL.put("[_a-z][_a-zA-Z0-9]*", "editor_xtl_functor");
		syntaxClassesForXTL.put(":-|!|-->|;|\\.", "editor_assignments");
		syntaxClassesForXTL.put("(%(.)*|/\\*([^*]|\\*+[^*/])*\\*+/)", "editor_comment");
		syntaxClassesForXTL.put("( |\t|\r|\n)+", "editor_ignored");

		//TLA Regex
		final Map<String, String> syntaxClassesForTLA = new LinkedHashMap<>();
		syntaxClassesForTLA.put("(MODULE|CONSTANTS|CONSTANT|ASSUME|ASSUMPTION|VARIABLES|VARIABLE|AXIOM|THEOREM|EXTENDS|INSTANCE|LOCAL)", "editor_keyword");
		syntaxClassesForTLA.put("(IF|THEN|ELSE|UNION|CHOOSE|LET|IN|UNCHANGED|SUBSET|CASE|DOMAIN|EXCEPT|ENABLED|SF_|WF_|WITH|OTHER|BOOLEAN|STRING)", "editor_ctrlkeyword");
		syntaxClassesForTLA.put("(Next|Init|Spec|Inv)", "editor_types");
		syntaxClassesForTLA.put("(\\\\\\*[^\n\r]*)|(\\(\\*([^*]|\\*+[^*)])*\\*+\\))", "editor_comment");
		syntaxClassesForTLA.put("\\+|=|-|\\*|\\^|/|\\.\\.|\\\\o|\\\\circ|\\\\div|\\\\leq|\\\\geq|%|<|>|/|Int|Nat", "editor_arithmetic");
		syntaxClassesForTLA.put("<=>|=>|<<|>>|!|#|/=|~|<>|->|->|~\\\\|\"|\\[\\]|TRUE|FALSE|SubSeq|Append|Len|Seq|Head|Tail|Cardinality|IsFiniteSet|/\\\\|\\\\/|\\\\land|\\\\lor|\\\\lnot|\\\\neg|\\\\equiv|\\\\E|\\\\A|\\\\in|\\\\notin|\\\\cap|\\\\intersect|\\\\cup|\\\\subseteq|\\\\subset|\\\\times|\\\\union|\\.|\\\\", "editor_logical");
		syntaxClassesForTLA.put("[_a-zA-Z][_a-zA-Z0-9]*", "editor_identifier");
		syntaxClassesForTLA.put("( |\t|\r|\n)+", "editor_ignored");

		//CSP Regex
		final Map<String, String> syntaxClassesForCSP = new LinkedHashMap<>();
		syntaxClassesForCSP.put("if|then|else|@@|let|within|\\{|\\}|<->|<-|\\[\\||\\|\\]|\\[|\\]|\\\\", "editor_keyword");
		syntaxClassesForCSP.put("!|\\?|->|\\[\\]|\\|~\\||\\|\\|\\||;|STOP|SKIP|CHAOS|/\\|\\[>|@", "editor_types");
		syntaxClassesForCSP.put("agent|MAIN|channel|datatype|subtype|nametype|machine|Events", "editor_arithmetic");
		syntaxClassesForCSP.put("assert|transparent|diamond|print|include", "editor_assignments");
		syntaxClassesForCSP.put("true|false|length|null|head|tail|concat|set|Set|Seq|elem|empty|card|member|union|diff|inter|Union|Inter|not|and|or|mod|\\*|\\+|/|==|\\!=|>|<|<=|>=|=<|&&|\\|\\||Int|Bool", "editor_logical");
		syntaxClassesForCSP.put("external|extensions|productions|Proc", "editor_unsupported");
		syntaxClassesForCSP.put("[_a-zA-Z][_a-zA-Z0-9]*", "editor_identifier");
		syntaxClassesForCSP.put("(\\{-([^-]|-+[^\\-\\}])*-+\\})|(--(.*))", "editor_comment");
		syntaxClassesForCSP.put("( |\t|\r|\n)+", "editor_ignored");

		//Alloy Regex
		final Map<String, String> syntaxClassesForAlloy = new LinkedHashMap<>();
		syntaxClassesForAlloy.put("module|sig|fact|extends|run|abstract|open|fun|pred|check|assert|plus|minus|mul|div|rem|sum", "editor_keyword");
		syntaxClassesForAlloy.put("not|one|lone|set|no|all|some|disjoint|let|in|for|and|or|implies|iff|else|none|univ|iden|Int|int|=>|&&|<=>|\\|\\||!|\\.|\\^|\\*|<:|:>|\\+\\+|\\~|->|&|\\+|-|=|\\#", "editor_types");
		syntaxClassesForAlloy.put("[_a-zA-Z][_a-zA-Z0-9]*", "editor_identifier");
		syntaxClassesForAlloy.put("(//[^\n\r]*|/\\*([^*]|\\*+[^*/])*\\*+/)", "editor_comment");

		//Z Regex
		final Map<String, String> syntaxClassesForZ = new LinkedHashMap<>();
		syntaxClassesForZ.put("(head|tail|last|front|squash|rev|min|max|first|second|succ|count|items|\\\\(\\{|\\}|notin|in|inbag|(big)?cup|(big)?cap|subset|subseteq|subbageq|disjoint|partition|plus|oplus|uplus|uminus|otimes|setminus|emptyset|leq|geq|neq|div|mod|dom|(n)?(d|r)res|langle|rangle|lbag|rbag|ran|id|inv|mapsto|succ|cat|dcat|prefix|suffix|inseq|filter|extract|bcount|\\#))", "editor_arithmetic");
		syntaxClassesForZ.put("\\\\(power(_1)?|nat(_1)?|num|bag|cross|upto|rel|(p)?fun|(p)?inj|bij|seq(_1)?|iseq(_1)?|(b)?tree)", "editor_types");
		syntaxClassesForZ.put("\\\\(land|lor|implies|iff|lnot|forall|exists(_1)?|mu|lambda|true|false)", "editor_logical");
		syntaxClassesForZ.put("(\\\\(where|also|Delta))|(\\\\(begin|end)\\{(schema|zed|axdef)\\})", "editor_keyword");
		syntaxClassesForZ.put("::=|=|\\\\(IF|THEN|ELSE|LET|defs)", "editor_assignments");
		syntaxClassesForZ.put("(\\\\|[_a-zA-Z])[_a-zA-Z0-9]*", "editor_identifier");
		syntaxClassesForZ.put("%(.)*|/\\*([^*]|\\*+[^*/])*\\*+/|\\\\(noindent|documentclass|(begin|end)\\{(document)\\}|(sub)?section|(usepackage)\\{(fuzz|z-eves)\\}|\\\\)", "editor_comment");
		syntaxClassesForZ.put("\\\\(infix|arithmos)", "editor_unsupported");

		syntaxClassesOtherLanguages.put(EventBFactory.class, syntaxClassesForEventB);
		syntaxClassesOtherLanguages.put(EventBPackageFactory.class, syntaxClassesForEventB);
		syntaxClassesOtherLanguages.put(XTLFactory.class, syntaxClassesForXTL);
		syntaxClassesOtherLanguages.put(TLAFactory.class, syntaxClassesForTLA);
		syntaxClassesOtherLanguages.put(CSPFactory.class, syntaxClassesForCSP);
		syntaxClassesOtherLanguages.put(AlloyFactory.class, syntaxClassesForAlloy);
		syntaxClassesOtherLanguages.put(ZFactory.class, syntaxClassesForZ);
	}

	static boolean canHighlight(final Class<? extends ModelFactory<?>> modelFactoryClass) {
		return syntaxClassesOtherLanguages.containsKey(modelFactoryClass);
	}

	private static LinkedList<Range> extractRanges(Map<String, String> syntaxClasses, String text) {
		LinkedList<Range> range = new LinkedList<>();
		syntaxClasses.forEach((key, syntaxClass) -> {
			Pattern pattern = Pattern.compile(key);
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				String adjustedSyntaxClass;
				if ("editor_identifier".equals(syntaxClass) && Utils.isProBSpecialDefinitionName(matcher.group())) {
					// Recognize and highlight special identifiers (e. g. ANIMATION_FUNCTION, VISB_JSON_FILE)
					adjustedSyntaxClass = "editor_special_identifier";
				} else {
					adjustedSyntaxClass = syntaxClass;
				}
				range.add(new Range(adjustedSyntaxClass, matcher.start(), matcher.end()));
			}
		});
		range.sort(Comparator.comparing(Range::getStart));
		return range;
	}

	private static LinkedList<Range> filterLongestMatch(LinkedList<Range> range) {
		LinkedList<Range> rangeWithLongestMatch = new LinkedList<>();
		int i = 0;
		while (i < range.size()) {
			int j = i + 1;
			int longestIndex = i;
			while (j < range.size()) {
				Range current = range.get(longestIndex);
				Range next = range.get(j);
				if (current.start != next.start) {
					break;
				}
				if (next.end > current.end) {
					longestIndex = j;
				}
				j++;
			}
			i = j;
			rangeWithLongestMatch.add(range.get(longestIndex));
		}
		return rangeWithLongestMatch;
	}

	private static StyleSpans<Collection<String>> createSpansBuilder(LinkedList<Range> ranges, String text) {
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		String currentText = text;
		int pos = 0;
		while (!currentText.isEmpty() && !ranges.isEmpty()) {
			Range first = ranges.getFirst();
			if (pos == first.getStart()) {
				int length = first.end - first.start;
				spansBuilder.add(Collections.singleton(first.getSyntaxClass()), length);
				pos = first.end;
				currentText = currentText.substring(length);
				ranges.removeFirst();
			} else if (pos > first.getStart()) {
				ranges.removeFirst();
			} else {
				int length = first.start - pos;
				spansBuilder.add(Collections.singleton("ignored"), length);
				pos = first.start;
				currentText = currentText.substring(length);
			}
		}

		try {
			return spansBuilder.create();
		} catch (IllegalStateException ignored) {
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	static StyleSpans<Collection<String>> computeHighlighting(final Class<? extends ModelFactory<?>> modelFactoryClass, final String text) {
		Map<String, String> syntaxClasses = syntaxClassesOtherLanguages.get(modelFactoryClass);
		LinkedList<Range> ranges = extractRanges(syntaxClasses, text);
		LinkedList<Range> rangesWithLongestMatch = filterLongestMatch(ranges);
		return createSpansBuilder(rangesWithLongestMatch, text);
	}
}
