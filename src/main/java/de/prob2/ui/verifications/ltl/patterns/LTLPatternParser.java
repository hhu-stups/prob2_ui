package de.prob2.ui.verifications.ltl.patterns;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.semantic.PatternDefinition;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import javax.inject.Inject;
import java.util.stream.Collectors;

@Singleton
public class LTLPatternParser {
	
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
		machine.getPatternManager().getPatterns().add(pattern);
	}
	
	private LTLParseListener checkDefinition(Pattern pattern, Machine machine) {
		LTLParseListener parseListener = initializeParseListener(pattern);
		pattern.updateDefinitions(machine.getPatternManager());
		pattern.setName(String.join("/", pattern.getDefinitions().stream()
				.map(PatternDefinition::getSimpleName)
				.collect(Collectors.toList())));
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
