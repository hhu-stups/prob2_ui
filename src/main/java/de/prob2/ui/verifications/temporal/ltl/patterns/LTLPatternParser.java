package de.prob2.ui.verifications.temporal.ltl.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.ltl.parser.LtlParser.NumVarParamContext;
import de.prob.ltl.parser.LtlParser.Pattern_defContext;
import de.prob.ltl.parser.LtlParser.Pattern_def_paramContext;
import de.prob.ltl.parser.LtlParser.SeqVarParamContext;
import de.prob.ltl.parser.LtlParser.VarParamContext;
import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.temporal.TemporalCheckingResultItem;
import de.prob2.ui.verifications.temporal.ltl.LTLParseListener;

import org.antlr.v4.runtime.tree.ParseTree;

public final class LTLPatternParser {
	private LTLPatternParser() {
		throw new AssertionError("Utility class");
	}
	
	private static void handlePatternResult(LTLParseListener parseListener, LTLPatternItem item) {
		CheckingResultItem resultItem;
		// Empty Patterns do not have parse errors which is a little bit confusing
		if(parseListener.getErrorMarkers().isEmpty() && !item.getCode().isEmpty()) {
			resultItem = new TemporalCheckingResultItem(Checked.SUCCESS, parseListener.getErrorMarkers(), "verifications.result.patternParsedSuccessfully");
		} else {
			List<ErrorItem> errorMarkers = parseListener.getErrorMarkers();
			if(item.getCode().isEmpty()) {
				resultItem = new TemporalCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "verifications.temporal.ltl.pattern.empty");
			} else {
				final String msg = parseListener.getErrorMarkers().stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
				resultItem = new TemporalCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "verifications.temporal.ltl.pattern.couldNotParsePattern", msg);
			}
		}
		item.setResultItem(resultItem);
	}
	
	public static LTLPatternItem parsePattern(final String description, final String code, final Machine machine) {
		final Pattern pattern = makePattern(description, code);
		final LTLParseListener parseListener = checkDefinition(pattern, machine);
		final LTLPatternItem item = new LTLPatternItem(pattern.getName(), pattern.getDescription(), pattern.getCode());
		handlePatternResult(parseListener, item);
		return item;
	}
	
	public static void addPattern(LTLPatternItem item, Machine machine) {
		Pattern pattern = makePattern(item.getDescription(), item.getCode());
		handlePatternResult(checkDefinition(pattern, machine), item);
		machine.getMachineProperties().getPatternManager().getPatterns().add(pattern);
	}
	
	private static LTLParseListener checkDefinition(Pattern pattern, Machine machine) {
		LTLParseListener parseListener = initializeParseListener(pattern);
		pattern.updateDefinitions(machine.getMachineProperties().getPatternManager());
		ParseTree ast = pattern.getAst();
		List<String> patternNames = new ArrayList<>();
		for(int i = 0; i < ast.getChildCount(); i++) {
			ParseTree child = ast.getChild(i);
			if(child instanceof Pattern_defContext && ((Pattern_defContext) child).ID() != null) {
				patternNames.add(extractPatternSignature(child));
			}
		}
		pattern.setName(String.join("\n", patternNames));
		return parseListener;
	}
	
	private static String extractPatternSignature(ParseTree tree) {
		StringBuilder signature = new StringBuilder();
		String name = ((Pattern_defContext) tree).ID().getText();
		signature.append(name);
		signature.append("(");
		signature.append(String.join(", ", ((Pattern_defContext) tree).pattern_def_param().stream()
			.map(LTLPatternParser::extractParameterFromContext)
			.collect(Collectors.toList())));
		signature.append(")");
		return signature.toString();
	}
	
	private static String extractParameterFromContext(Pattern_def_paramContext ctx) {
		if(ctx instanceof VarParamContext) {
			return ((VarParamContext) ctx).ID().getText();
		} else if(ctx instanceof NumVarParamContext) {
			return ((NumVarParamContext) ctx).ID().getText();
		} else if(ctx instanceof SeqVarParamContext) {
			return ((SeqVarParamContext) ctx).ID().getText();
		}
		return "";
	}
	
	private static LTLParseListener initializeParseListener(Pattern pattern) {
		LTLParseListener parseListener = new LTLParseListener();
		pattern.removeErrorListeners();
		pattern.removeWarningListeners();
		pattern.removeUpdateListeners();
		pattern.addErrorListener(parseListener);
		return parseListener;
	}
	
	public static void removePattern(LTLPatternItem item, Machine machine) {
		PatternManager patternManager = machine.getMachineProperties().getPatternManager();
		Pattern pattern = patternManager.getUserPattern(item.getName());
		if(pattern != null) {
			patternManager.removePattern(pattern);
		}
	}
	
	private static Pattern makePattern(final String description, final String code) {
		final Pattern pattern = new Pattern();
		pattern.setBuiltin(false);
		pattern.setDescription(description);
		pattern.setCode(code);
		return pattern;
	}
	
	public static void parseMachine(Machine machine) {
		machine.getMachineProperties().getLTLPatterns().forEach(item -> LTLPatternParser.addPattern(item, machine));
	}
	
}
