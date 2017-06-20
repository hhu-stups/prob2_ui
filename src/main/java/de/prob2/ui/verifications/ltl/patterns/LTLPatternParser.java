package de.prob2.ui.verifications.ltl.patterns;


import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
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
		
	public void parsePattern(LTLPatternItem item, PatternManager patternManager) {
		logger.trace("Parse ltl pattern");
		Pattern pattern = new Pattern();
		pattern.setBuiltin(false);
		pattern.setName(item.getName());
		pattern.setDescription(item.getDescription());
		pattern.setCode(item.getCode());
		patternManager.getPatterns().add(pattern);
		LTLParseListener parseListener = new LTLParseListener();
		pattern.removeErrorListeners();
		pattern.removeWarningListeners();
		pattern.removeUpdateListeners();
		pattern.addErrorListener(parseListener);
		pattern.addWarningListener(parseListener);
		pattern.addUpdateListener(parseListener);
		pattern.updateDefinitions(patternManager);
		resultHandler.handlePatternResult(parseListener, item);
		injector.getInstance(LTLView.class).refreshPattern();
	}
	
}
