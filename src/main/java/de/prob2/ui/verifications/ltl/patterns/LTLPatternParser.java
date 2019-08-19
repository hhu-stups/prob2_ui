package de.prob2.ui.verifications.ltl.patterns;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.LtlParser.NumVarParamContext;
import de.prob.ltl.parser.LtlParser.Pattern_defContext;
import de.prob.ltl.parser.LtlParser.Pattern_def_paramContext;
import de.prob.ltl.parser.LtlParser.SeqVarParamContext;
import de.prob.ltl.parser.LtlParser.VarParamContext;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.ILTLItemHandler;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;

import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class LTLPatternParser implements ILTLItemHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
	
	private final LTLResultHandler resultHandler;
		
	@Inject
	private LTLPatternParser(final LTLResultHandler resultHandler) {
		this.resultHandler = resultHandler;
	}
		
	public void parsePattern(LTLPatternItem item, Machine machine) {
		logger.trace("Parse ltl pattern");
		Pattern pattern = itemToPattern(item);
		resultHandler.handlePatternResult(checkDefinition(pattern, machine), item);
		item.setName(pattern.getName());
	}
	
	public void addPattern(LTLPatternItem item, Machine machine) {
		Pattern pattern = itemToPattern(item);
		resultHandler.handlePatternResult(checkDefinition(pattern, machine), item);
		item.setName(pattern.getName());
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
		pattern.addWarningListener(parseListener);
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
	
	private Pattern itemToPattern(LTLPatternItem item) {
		Pattern pattern = new Pattern();
		pattern.setBuiltin(false);
		pattern.setDescription(item.getDescription());
		pattern.setCode(item.getCode());
		return pattern;
	}
	
	public void parseMachine(Machine machine) {
		machine.getLTLPatterns().forEach(item-> {
			this.parsePattern(item, machine);
			this.addPattern(item, machine);
		});
	}
	
}
