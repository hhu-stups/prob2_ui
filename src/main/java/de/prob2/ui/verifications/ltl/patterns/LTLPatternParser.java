package de.prob2.ui.verifications.ltl.patterns;


import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.LTLView;

public class LTLPatternParser {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
	
	private final LTLResultHandler resultHandler;
	
	private final Injector injector;
	
	@Inject
	private LTLPatternParser(final LTLResultHandler resultHandler, final Injector injector) {
		this.resultHandler = resultHandler;
		this.injector = injector;
	}
		
	public void parsePattern(LTLPatternItem item, Machine machine) {
		if(machine.getPatternManager() == null) {
			return;
		}
		logger.trace("Parse ltl pattern");
		Pattern pattern = itemToPattern(item);
		machine.getPatternManager().getPatterns().add(pattern);
		resultHandler.handlePatternResult(checkDefinition(pattern, machine), item);
		injector.getInstance(LTLView.class).refreshPattern();
	}
	
	private LTLParseListener checkDefinition(Pattern pattern, Machine machine) {
		LTLParseListener parseListener = new LTLParseListener();
		pattern.removeErrorListeners();
		pattern.removeWarningListeners();
		pattern.removeUpdateListeners();
		pattern.addErrorListener(parseListener);
		pattern.addWarningListener(parseListener);
		pattern.addUpdateListener(parseListener);
		pattern.updateDefinitions(machine.getPatternManager());
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
		machine.getPatterns().forEach(item-> {
			this.parsePattern(item, machine);
		});
	}
	
}
