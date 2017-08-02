package de.prob2.ui.verifications.ltl.patterns;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class LTLPatternParser {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
	
	private final LTLResultHandler resultHandler;
		
	@Inject
	private LTLPatternParser(final LTLResultHandler resultHandler) {
		this.resultHandler = resultHandler;
	}
		
	public void parsePattern(LTLPatternItem item, Machine machine, boolean byInit) {
		logger.trace("Parse ltl pattern");
		Pattern pattern = itemToPattern(item);
		machine.getPatternManager().getPatterns().add(pattern);
		resultHandler.handlePatternResult(checkDefinition(pattern, machine), item, byInit);
	}
	
	private LTLParseListener checkDefinition(Pattern pattern, Machine machine) {
		LTLParseListener parseListener = initializeParseListener(pattern);
		pattern.updateDefinitions(machine.getPatternManager());
		return parseListener;
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
		patternManager.removePattern(patternManager.getUserPattern(item.getName()));
	}
	
	private Pattern itemToPattern(LTLPatternItem item) {
		Pattern pattern = new Pattern();
		pattern.setBuiltin(false);
		pattern.setName(item.getName());
		pattern.setDescription(item.getDescription());
		pattern.setCode(item.getCode());
		return pattern;
	}
	
	public void parseMachine(Machine machine) {
		machine.getPatterns().forEach(item-> this.parsePattern(item, machine, true));
	}
	
}
