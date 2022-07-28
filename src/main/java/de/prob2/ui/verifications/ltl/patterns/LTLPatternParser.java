package de.prob2.ui.verifications.ltl.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.ltl.parser.LtlParser.NumVarParamContext;
import de.prob.ltl.parser.LtlParser.Pattern_defContext;
import de.prob.ltl.parser.LtlParser.Pattern_def_paramContext;
import de.prob.ltl.parser.LtlParser.SeqVarParamContext;
import de.prob.ltl.parser.LtlParser.VarParamContext;
import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;

import org.antlr.v4.runtime.tree.ParseTree;

@Singleton
public class LTLPatternParser {
	private final LTLResultHandler resultHandler;
		
	@Inject
	private LTLPatternParser(final LTLResultHandler resultHandler) {
		this.resultHandler = resultHandler;
	}
	
	public LTLPatternItem parsePattern(final String description, final String code, final Machine machine) {
		final Pattern pattern = makePattern(description, code);
		final LTLParseListener parseListener = checkDefinition(pattern, machine);
		final LTLPatternItem item = new LTLPatternItem(pattern.getName(), pattern.getDescription(), pattern.getCode());
		resultHandler.handlePatternResult(parseListener, item);
		return item;
	}
	
	public void addPattern(LTLPatternItem item, Machine machine) {
		Pattern pattern = itemToPattern(item);
		resultHandler.handlePatternResult(checkDefinition(pattern, machine), item);
		machine.getPatternManager().getPatterns().add(pattern);
	}
	
	private LTLParseListener checkDefinition(Pattern pattern, Machine machine) {
		LTLParseListener parseListener = initializeParseListener(pattern);
		pattern.updateDefinitions(machine.getPatternManager());
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
	
	private String extractPatternSignature(ParseTree tree) {
		StringBuilder signature = new StringBuilder();
		String name = ((Pattern_defContext) tree).ID().getText();
		signature.append(name);
		signature.append("(");
		signature.append(String.join(", ", ((Pattern_defContext) tree).pattern_def_param().stream()
			.map(this::extractParameterFromContext)
			.collect(Collectors.toList())));
		signature.append(")");
		return signature.toString();
	}
	
	private String extractParameterFromContext(Pattern_def_paramContext ctx) {
		if(ctx instanceof VarParamContext) {
			return ((VarParamContext) ctx).ID().getText();
		} else if(ctx instanceof NumVarParamContext) {
			return ((NumVarParamContext) ctx).ID().getText();
		} else if(ctx instanceof SeqVarParamContext) {
			return ((SeqVarParamContext) ctx).ID().getText();
		}
		return "";
	}
	
	private LTLParseListener initializeParseListener(Pattern pattern) {
		LTLParseListener parseListener = new LTLParseListener();
		pattern.removeErrorListeners();
		pattern.removeWarningListeners();
		pattern.removeUpdateListeners();
		pattern.addErrorListener(parseListener);
		pattern.addUpdateListener(parseListener);
		return parseListener;
	}
	
	public void removePattern(LTLPatternItem item, Machine machine) {
		PatternManager patternManager = machine.getPatternManager();
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
	
	private Pattern itemToPattern(LTLPatternItem item) {
		return makePattern(item.getDescription(), item.getCode());
	}
	
	public void parseMachine(Machine machine) {
		machine.getLTLPatterns().forEach(item -> this.addPattern(item, machine));
	}
	
}
